package com.goudong.message.controller;

import com.goudong.commons.enumerate.ClientExceptionEnum;
import com.goudong.commons.enumerate.RedisKeyEnum;
import com.goudong.commons.exception.BasicException;
import com.goudong.commons.pojo.Result;
import com.goudong.commons.utils.AssertUtil;
import com.goudong.commons.utils.RedisValueUtil;
import com.goudong.message.config.CodeDirectRabbitConfig;
import com.goudong.message.sender.EmailCodeRoutingKeySender;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.Email;

/**
 * 类描述：
 * 邮箱控制器
 * @RefreshScope  可以使当前类下的配置支持动态更新。
 * @Author msi
 * @Date 2021-05-05 10:59
 * @Version 1.0
 */
@Api(tags="验证码")
@Validated
@RestController
@RequestMapping("/code")
@RefreshScope
public class CodeController {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RedisValueUtil redisValueUtil;

    @Resource
    private EmailCodeRoutingKeySender emailCodeRoutingKeySender;

    @GetMapping("/demo")
    @ApiOperation("测试")
    public Result demo () {
        BasicException.exception(ClientExceptionEnum.UNAUTHORIZED);
        return Result.ofSuccess();
    }

    /**
     * 发送邮箱验证码
     * @return
     */
    @GetMapping("/email-code/{email}")
    @ApiOperation(value = "发送邮箱验证码")
    @ApiImplicitParam(name = "email", value = "邮箱", required = true)
    public Result sendEmailCode (@PathVariable("email") @Email(message = "请输入正确邮箱格式") String email) {
        rabbitTemplate.convertAndSend(CodeDirectRabbitConfig.CODE_DIRECT_EXCHANGE, CodeDirectRabbitConfig.EMAIL_CODE_ROUTING_KEY, email);
        return Result.ofSuccess();
    }

    /**
     * 发送手机验证码
     * @return
     */
    @GetMapping("/phone-code/{phone}")
    @ApiOperation(value = "发送手机验证码")
    @ApiImplicitParam(name = "phone", value = "手机号码", required = true)
    public Result sendPhoneCode (@PathVariable("phone") String phone) {
        AssertUtil.isPhone(phone, "手机号格式错误：" + phone);
        rabbitTemplate.convertAndSend(CodeDirectRabbitConfig.CODE_DIRECT_EXCHANGE, CodeDirectRabbitConfig.PHONE_CODE_ROUTING_KEY, phone);
        return Result.ofSuccess();
    }

    /**
     * 验证验证码是否正确
     * @param number 账号
     * @param code 验证码
     * @return
     */
    @GetMapping("/check-code/{number}/{code}")
    @ApiOperation(value = "验证验证码是否正确")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "number", value = "手机号码/邮箱", required = true),
        @ApiImplicitParam(name = "code", value = "验证码", required = true)
    })
    public Result<Boolean> checkCode (@PathVariable String number, @PathVariable String code) {
        String redisValue = redisValueUtil.getValue(RedisKeyEnum.MESSAGE_AUTH_CODE, number);
        if (redisValue == null || !code.equals(redisValue)) {
            return Result.ofSuccess(false);
        }
        return Result.ofSuccess(true);
    }
}
