package com.reviewPointMangeServer.reviewPointMangeServer.service;

import com.reviewPointMangeServer.reviewPointMangeServer.dto.requestDto.jwt.TokenRequestDto;
import com.reviewPointMangeServer.reviewPointMangeServer.dto.requestDto.user.UserLoginRequestDto;
import com.reviewPointMangeServer.reviewPointMangeServer.dto.requestDto.user.UserRegisterRequestDto;
import com.reviewPointMangeServer.reviewPointMangeServer.dto.responseDto.jwt.TokenResponseDto;
import com.reviewPointMangeServer.reviewPointMangeServer.dto.responseDto.user.UserGetResponseDto;
import com.reviewPointMangeServer.reviewPointMangeServer.dto.responseDto.user.UserRegisterResponseDto;
import com.reviewPointMangeServer.reviewPointMangeServer.exception.*;
import com.reviewPointMangeServer.reviewPointMangeServer.model.User;
import com.reviewPointMangeServer.reviewPointMangeServer.repository.UserRepository;
import com.reviewPointMangeServer.reviewPointMangeServer.config.jwt.JwtTokenProvider;
import com.reviewPointMangeServer.reviewPointMangeServer.dto.responseDto.user.UserLoginResponseDto;
import com.reviewPointMangeServer.reviewPointMangeServer.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SignService {

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    // DTO로 들어온 값을 통해 회원가입을 진행
    @Transactional
    public UserRegisterResponseDto registerUser(UserRegisterRequestDto requestDto) {
        validateDuplicated(requestDto.getUserName());
        User user = userRepository.save(
                User.builder()
                        .userName(requestDto.getUserName())
                        .userPw(passwordEncoder.encode(requestDto.getUserPw()))
                        .build());
        return UserRegisterResponseDto.builder()
                .userName(user.getUserName())
                .userPw(user.getUserPw())
                .build();
    }

    public void validateDuplicated(String userName) {
        if (userRepository.findByUserName(userName).isPresent()) throw new UserIdAlreadyExistsException();
    }

    // 로그인 구현
    @Transactional
    public UserLoginResponseDto loginUser(UserLoginRequestDto requestDto) {
        User user = userRepository.findByUserName(requestDto.getUserName()).orElseThrow(UserNotFoundException::new);
        if(!passwordEncoder.matches(requestDto.getUserPw(), user.getUserPw()))
            throw new LoginFailureException();
        user.updateRefreshToken(jwtTokenProvider.createRefreshToken());
        return new UserLoginResponseDto(user.getUserId(), jwtTokenProvider.createToken(requestDto.getUserName()), user.getRefreshToken());
    }

    // 토큰 재발행
    @Transactional
    public TokenResponseDto reIssue(TokenRequestDto requestDto) {
        if(!jwtTokenProvider.validateTokenExpiration(requestDto.getRefreshToken()))
            throw new InvalidRefreshTokenException();
        User user = findUserByToken(requestDto);

        if(!user.getRefreshToken().equals(requestDto.getRefreshToken()))
            throw new InvalidRefreshTokenException();
        String accessToken = jwtTokenProvider.createToken(user.getUserName());
        String refreshToken = jwtTokenProvider.createRefreshToken();
        user.updateRefreshToken(refreshToken);
        return new TokenResponseDto(accessToken, refreshToken);
    }
    public User findUserByToken(TokenRequestDto requestDto) {
        Authentication auth = jwtTokenProvider.getAuthentication(requestDto.getAccessToken());
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String userName = userDetails.getUsername();
        return userRepository.findByUserName(userName).orElseThrow(UserNotFoundException::new);
    }

    public UserGetResponseDto getUserUser(UUID userId){
        User user = findByUser(userId);
        return UserGetResponseDto.builder()
                .userId(userId)
                .userName(user.getUserName())
                .point(user.getPoint())
                .userLevel(user.getUserLevel())
                .build();
    }

    private User findByUser(UUID userId) {
        return userRepository.findUserByUserId(userId).orElseThrow(PlaceNotFoundException::new);
    }

}
