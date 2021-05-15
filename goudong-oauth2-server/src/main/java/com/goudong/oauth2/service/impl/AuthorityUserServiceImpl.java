package com.goudong.oauth2.service.impl;

import cn.hutool.core.util.IdUtil;
import com.goudong.commons.dto.AuthorityUserDTO;
import com.goudong.commons.enumerate.ClientExceptionEnumInterface;
import com.goudong.commons.exception.BasicException;
import com.goudong.commons.po.AuthorityRolePO;
import com.goudong.commons.po.AuthorityUserPO;
import com.goudong.commons.po.AuthorityUserRolePO;
import com.goudong.commons.utils.AssertUtil;
import com.goudong.commons.utils.BeanUtil;
import com.goudong.oauth2.dao.AuthorityRoleDao;
import com.goudong.oauth2.dao.AuthorityUserDao;
import com.goudong.oauth2.dao.AuthorityUserRoleDao;
import com.goudong.oauth2.service.AuthorityUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 类描述：
 * 用户服务层
 * @Author msi
 * @Date 2021-05-02 13:53
 * @Version 1.0
 */
@Slf4j
@Service
public class AuthorityUserServiceImpl implements AuthorityUserService {

    @Resource
    private AuthorityUserDao authorityUserDao;
    @Resource
    private AuthorityRoleDao authorityRoleDao;
    @Resource
    private AuthorityUserRoleDao authorityUserRoleDao;

    /**
     * 根据 authorityUserPO对象，查询 authority_user表
     *
     * @param authorityUserDTO 用户对象
     * @return
     */
    @Override
    public List<AuthorityUserDTO> listByAuthorityUserDTO(AuthorityUserDTO authorityUserDTO) {
        AuthorityUserPO authorityUserPO = new AuthorityUserPO();
        // 转换对象
        BeanUtils.copyProperties(authorityUserDTO, authorityUserPO);
        // 查询
        List<AuthorityUserPO> authorityUserPOS = authorityUserDao.selectByAnd(authorityUserPO);

        return BeanUtil.copyList(authorityUserPOS, AuthorityUserDTO.class);
    }

    /**
     * 根据指定的用户名，生成3个可以未被注册的用户名
     * 当返回结果为空集合时，表示账号可以使用
     * @param username
     * @return
     */
    @Override
    public List<String> generateUserName(String username) {
        AssertUtil.hasText(username, "根据用户名查询用户时，用户名不能为空");
        List<String> result = new ArrayList<>();
        // 查询用户名是否存在
        AuthorityUserPO authorityUserPO = new AuthorityUserPO();
        authorityUserPO.setUsername(username);
        List<AuthorityUserPO> authorityUserPOS = authorityUserDao.selectByAnd(authorityUserPO);

        if (authorityUserPOS.isEmpty()) {
            return result;
        }

        List<String> names = authorityUserDao.selectUserNameByLikeUsername(username);

        Random random = new Random();
        do {
            int i = random.nextInt(10000);
            String item = username + i;
            // 本次生成的用户名不存在
            if(!names.contains(item)){
                names.add(item);
                result.add(item);
            }
        } while (result.size() < 3);

        return result;
    }


    /**
     * 新增用户
     *
     * @param authorityUserDTO
     * @return
     */
    @Transactional
    @Override
    public AuthorityUserDTO createUser(AuthorityUserDTO authorityUserDTO) {

        AuthorityUserPO userPO = (AuthorityUserPO) BeanUtil.copyProperties(authorityUserDTO, AuthorityUserPO.class);

        List<AuthorityUserPO> userPOList = authorityUserDao.selectByOr(userPO);

        if (userPOList.size() > 1) {
            // 有多条，表示提交的数据有问题
            // 1. 使用postman 类似工具，提交未经校验的内容
            // 2. 注册时间过长，账号被别人注册了
            BasicException.exception(ClientExceptionEnumInterface.BAD_REQUEST);
        }
        String accountRadio = authorityUserDTO.getAccountRadio();
        // 为空，插入
        if (userPOList.isEmpty() && "".equals(accountRadio)) {
            userPO.setPassword(BCrypt.hashpw(userPO.getPassword(), BCrypt.gensalt()));
            userPO.setUuid(IdUtil.randomUUID());
            authorityUserDao.insert(userPO);

            AuthorityRolePO rolePO = authorityRoleDao.select(AuthorityRolePO.builder().roleName("ROLE_ORDINARY").build()).get(0);
            // 绑定角色
            AuthorityUserRolePO userRolePO = AuthorityUserRolePO.builder().roleUuid(rolePO.getUuid()).userUuid(userPO.getUuid()).build();

            authorityUserRoleDao.insert(userRolePO);

            return (AuthorityUserDTO) BeanUtil.copyProperties(userPO, AuthorityUserDTO.class);
        }

        if ("MY_SELF".equals(accountRadio) || "NOT_MY_SELF".equals(accountRadio)) {
            // 数据库没有相关用户有一条数据
            userPO.setUuid(userPOList.get(0).getUuid());

            // 加密
            userPO.setPassword(BCrypt.hashpw(userPO.getPassword(), BCrypt.gensalt()));
            authorityUserDao.updateInsert(userPO);

            return (AuthorityUserDTO) BeanUtil.copyProperties(userPO, AuthorityUserDTO.class);
        }

        BasicException.exception(ClientExceptionEnumInterface.BAD_REQUEST);

        return null;
    }
}
