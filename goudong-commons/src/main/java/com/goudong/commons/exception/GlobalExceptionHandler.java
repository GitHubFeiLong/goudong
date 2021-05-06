package com.goudong.commons.exception;


import com.goudong.commons.enumerate.ClientExceptionEnum;
import com.goudong.commons.enumerate.ServerExceptionEnum;
import com.goudong.commons.pojo.Result;
import com.goudong.commons.pojo.Url;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Not;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局捕获异常
 * 1.捕获返回json格式
 * 2.捕获返回页面
 * @ClassName GlobalExceptionHandler
 * @Author msi
 * @Date 2019/7/28 21:51
 */
@Slf4j
//@RestControllerAdvice(basePackages = "com.goudong")
@ControllerAdvice
@RestController
public class GlobalExceptionHandler {
    /**
     * 错误日志模板
     */
    public static final String LOG_ERROR_INFO = "http响应码：{}，错误代码：{}，客户端错误信息：{}，服务端错误信息：{}";

    /**
     * 请求对象
     */
    @Resource
    private HttpServletRequest request;

    /**
     * 响应对象
     */
    @Resource
    private HttpServletResponse response;


    /**
     * 全局处理客户端错误
     * @param exception      客户端异常
     * @return
     */
    @ExceptionHandler(BasicException.class)
    public Result<BasicException> clientExceptionDispose(BasicException exception){
        // 设置响应码
        response.setStatus(exception.getStatus());
        // 打印错误日志
        log.error(GlobalExceptionHandler.LOG_ERROR_INFO, exception.getStatus(), exception.getCode(), exception.getClientMessage(), exception.getServerMessage());
        // 堆栈跟踪
        exception.printStackTrace();

        return Result.ofFail(exception);
    }

    /**
     * Spring Validation 注解异常
     * @param e
     * @return
     */
    @ExceptionHandler(value = {BindException.class, ConstraintViolationException.class, MethodArgumentNotValidException.class})
    public Result<Throwable> ValidExceptionDispose(Exception e){
        BasicException clientException = new BasicException.ClientException(ClientExceptionEnum.PARAMETER_ERROR);
        this.response.setStatus(clientException.status);
        List<String> messages = new ArrayList<>();
        if (e instanceof BindException) {
            List<ObjectError> list = ((BindException) e).getAllErrors();
            for (ObjectError item : list) {
                messages.add(item.getDefaultMessage());
            }
        } else if (e instanceof ConstraintViolationException) {
            for (ConstraintViolation<?> constraintViolation : ((ConstraintViolationException)e).getConstraintViolations()) {
                messages.add(constraintViolation.getMessage());
            }
        } else {
            messages.add(((MethodArgumentNotValidException)e).getBindingResult().getFieldError().getDefaultMessage());
        }

        String message = String.join(",", messages);
        // 打印错误日志
        log.error(GlobalExceptionHandler.LOG_ERROR_INFO, 400, "VALIDATION", message, e.getMessage());
        clientException.setClientMessage(message);
        clientException.setServerMessage(e.getMessage());
        // 堆栈跟踪
        e.printStackTrace();
        return Result.ofFail(clientException);
    }

    /**
     * 捕获意料之外的异常Exception
     * @param e
     * @return
     */
    @ExceptionHandler(Throwable.class)
    public Result<Throwable> otherErrorDispose(Throwable e){
        BasicException serverException = new BasicException.ServerException(ServerExceptionEnum.SERVER_ERROR);
        this.response.setStatus(serverException.status);
        // 打印错误日志
        log.error(GlobalExceptionHandler.LOG_ERROR_INFO, serverException.status, serverException.code, serverException.clientMessage, e.getMessage());
        // 堆栈跟踪
        e.printStackTrace();
        serverException.setServerMessage(e.getMessage());
        return Result.ofFail(serverException);
    }

    /*==========================================================================
        常见自定义异常
    ==========================================================================*/

    /**
     * 404 Not Found
     * 请求失败，请求所希望得到的资源未被在服务器上发现。没有信息能够告诉用户这个状况到底是暂时的还是永久的。假如服务器知道情况的话，应当使用410状态码来告知旧资源因为某些内部的配置机制问题，已经永久的不可用，而且没有任何可以跳转的地址。404这个状态码被广泛应用于当服务器不想揭示到底为何请求被拒绝或者没有其他适合的响应可用的情况下。出现这个错误的最有可能的原因是服务器端没有这个页面。
     * @return
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result notFound() {
        return Result.ofFailByNotFound(request.getRequestURL().toString(), null);
    }

    /**
     * 405 Method Not Allowed
     * 请求行中指定的请求方法不能被用于请求相应的资源。该响应必须返回一个Allow 头信息用以表示出当前资源能够接受的请求方法的列表。
     *
     * 鉴于 PUT，DELETE 方法会对服务器上的资源进行写操作，因而绝大部分的网页服务器都不支持或者在默认配置下不允许上述请求方法，对于此类请求均会返回405错误。
     * @return
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result methodNotAllowed() {
        return Result.ofFailByMethodNotAllowed(request.getRequestURL().toString(), null);
    }

}
