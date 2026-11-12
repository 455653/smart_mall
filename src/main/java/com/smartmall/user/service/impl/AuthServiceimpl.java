package com.smartmall.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;
import com.smartmall.common.security.JwtUtil;
import com.smartmall.common.security.LoginUser;
import com.smartmall.common.security.Role;
import com.smartmall.user.domain.UserAccount;
import com.smartmall.user.dto.LoginRequest;
import com.smartmall.user.dto.RefreshTokenRequest;
import com.smartmall.user.dto.RegisterRequest;
import com.smartmall.user.enums.AccountStatus;
import com.smartmall.user.mapper.UserAccountMapper;
import com.smartmall.user.service.AuthService;
import com.smartmall.user.service.LoginAttemptGuard;
import com.smartmall.user.service.MembershipPolicy;
import com.smartmall.user.vo.LoginResponse;
import com.smartmall.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final MembershipPolicy membershipPolicy;
    private final LoginAttemptGuard loginAttemptGuard;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String phone = request.phone().trim();
        // 先做一次友好校验，能直接给出"账号/手机号已注册"的提示；
        // 并发下仍可能同时通过，靠下面的唯一索引兜底。
        if (userAccountMapper.selectCount(Wrappers.<UserAccount>lambdaQuery()
                .eq(UserAccount::getUsername, username)) > 0) {
            throw new BizException(ResultCode.USERNAME_EXISTS, "账号已被注册");
        }
        if (userAccountMapper.selectCount(Wrappers.<UserAccount>lambdaQuery()
                .eq(UserAccount::getPhone, phone)) > 0) {
            throw new BizException(ResultCode.USERNAME_EXISTS, "手机号已被注册");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(username);
        userAccount.setPhone(phone);
        // 昵称不填则用手机尾号兜底，保证昵称非空
        userAccount.setNickname(nicknameOf(request.nickname(), phone));
        userAccount.setPassword(passwordEncoder.encode(request.password()));
        userAccount.setRole(Role.CUSTOMER);
        userAccount.setStatus(AccountStatus.ENABLED);
        userAccount.setLevel(membershipPolicy.leveOf(BigDecimal.ZERO));
        userAccount.setPoints(0);
        userAccount.setTotalConsumption(BigDecimal.ZERO);
        userAccount.setTokenVersion(0);
        try {
            userAccountMapper.insert(userAccount);
        } catch (DuplicateKeyException e) {
            // uk_user_username / uk_user_phone 唯一索引兜底
            throw new BizException(ResultCode.USERNAME_EXISTS, "账号或手机号已被注册");
        }

        log.info("新用户注册成功 userId={} username={}", userAccount.getId(), username);
        return issueTokens(userAccount);
    }

    @Override
    public LoginResponse login(LoginRequest request, String ip) {
        String account = request.account().trim();

        // 先看有没有被锁定（连续错 5 次锁 15 分钟），锁定期内直接拒绝
        long lockedSeconds = loginAttemptGuard.lockedSeconds(account);
        if (lockedSeconds > 0) {
            throw new BizException(ResultCode.RATE_LIMITED, "账号已锁定，请 " + lockedSeconds + " 秒后再试");
        }

        UserAccount user = userAccountMapper.selectByAccount(account);
        // 账号不存在与密码错误返回同一个错误码：避免攻击者靠错误码枚举出哪些账号真实存在
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptGuard.onFailure(account);
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }
        if (!user.enabled()) {
            throw new BizException(ResultCode.USER_DISABLED);
        }

        loginAttemptGuard.onSuccess(account);
        userAccountMapper.updateLastLogin(user.getId(), ip);
        log.info("用户登录成功 userId={} ip={}", user.getId(), ip);
        return issueTokens(user);
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        // 解析刷新令牌：签名、过期、typ=refresh 三重校验都在这里做掉
        LoginUser loginUser = jwtUtil.parseRefreshToken(request.refreshToken());
        UserAccount user = userAccountMapper.selectById(loginUser.userId());
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (!user.enabled()) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        // 令牌版本对不上说明已登出/改密，旧刷新令牌也不能再换新令牌
        if (user.getTokenVersion() != loginUser.tokenVersion()) {
            throw new BizException(ResultCode.TOKEN_INVALID, "登录已失效，请重新登录");
        }
        return issueTokens(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(Long userId) {
        // 令牌版本自增：已签发的 access / refresh 全部失效，不用维护黑名单
        userAccountMapper.bumpTokenVersion(userId);
        log.info("用户登出 userId={}", userId);
    }

    /** 签发一对令牌 + 组装登录响应 */
    private LoginResponse issueTokens(UserAccount userAccount) {
        String accessToken = jwtUtil.createAccessToken(userAccount.getId(), userAccount.getUsername(),
                userAccount.getRole(), userAccount.getTokenVersion());
        String refreshToken = jwtUtil.createRefreshToken(userAccount.getId(), userAccount.getUsername(),
                userAccount.getRole(), userAccount.getTokenVersion());
        return LoginResponse.of(accessToken, refreshToken,
                jwtUtil.accessTtlSeconds(), UserVO.of(userAccount));
    }

    /** 昵称为空时用手机尾号兜底：用户+0004 */
    private String nicknameOf(String nickname, String phone) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname.trim();
        }
        return "用户" + (phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone);
    }
}
