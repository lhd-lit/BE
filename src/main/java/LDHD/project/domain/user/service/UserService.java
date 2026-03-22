    package LDHD.project.domain.user.service;

    import LDHD.project.common.exception.GeneralException;
    import LDHD.project.common.response.ErrorCode;
    import LDHD.project.domain.group.repository.GroupDocumentRepository;
    import LDHD.project.domain.group.repository.GroupMemberRepository;
    import LDHD.project.domain.selfStudy.repository.SelfStudyRepository;
    import LDHD.project.domain.user.User;
    import LDHD.project.domain.user.repository.UserRepository;
    import LDHD.project.domain.user.web.controller.dto.*;
    import org.springframework.transaction.annotation.Transactional;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;

    import java.util.List;

    @Transactional(readOnly = true)
    @Service
    @RequiredArgsConstructor
    public class UserService {

        private final UserRepository userRepository;
        private final SelfStudyRepository  selfStudyRepository;
        private final GroupDocumentRepository groupDocumentRepository;
        private final GroupMemberRepository groupMemberRepository;
    /*
        // 회원 가입
        @Transactional // readOnly 기본 값: false
        public CreateUserResponse createUser(CreateUserRequest request) {

            // 로그인 ID 중복 검사
            if (userRepository.existsByLoginId(request.getLoginId())) {
                throw new GeneralException(ErrorCode.ALREADY_EXISTS);
            }

            // 이메일 중복 검사
            if(userRepository.existsByEmail(request.getEmail())) {
                throw new GeneralException(ErrorCode.ALREADY_EXISTS);
            }

            // 입력받은 년,월,일 로 LocalDate 생성
            LocalDate birthDate = LocalDate.of(
                    request.getBirthYear(),
                    request.getBirthMonth(),
                    request.getBirthDay()
            );

            User user = User.builder()
                    .name(request.getName())
                    .loginId(request.getLoginId())
                    .loginPassword(request.getLoginPassword())
                    .email(request.getEmail())
                    .address(request.getAddress())
                    .age(request.getAge())
                    .gender(request.getGender())
                    .birthDate(birthDate)
                    .build(); // 위에서 만든 변수 birthDate 사용 -> 년,월,일 한꺼번에 적용
            userRepository.save(user);

            return new CreateUserResponse(request.getName());

        }
    */
        // 회원 삭제
        @Transactional
        public DeleteUserResponse deleteUser(Long userId) {

            if(!userRepository.existsById(userId)) {
                throw new GeneralException(ErrorCode.USER_NOT_FOUND);

            }
                userRepository.deleteById(userId);

            return new DeleteUserResponse(userId);

        }
    /*
        //회원 정보 수정
        @Transactional
        public UpdateUserResponse updateUser(Long userId, UpdateUserRequest request) {

            User user = userRepository.findById(userId).orElseThrow(
                    ()-> new GeneralException(ErrorCode.USER_NOT_FOUND));

            LocalDate newBirthDate = LocalDate.of(
                    request.getBirthYear(),
                    request.getBirthMonth(),
                    request.getBirthDay()
            );

            user.setName(request.getName());
            user.setLoginId(request.getLoginId());
            user.setLoginPassword(request.getLoginPassword());
            user.setEmail(request.getEmail());
            user.setAddress(request.getAddress());
            user.setAge(request.getAge());
            user.setGender(request.getGender());
            user.setBirthDate(newBirthDate);

            userRepository.save(user);

            return new UpdateUserResponse(
                    user.getId(),
                    user.getName(),
                    user.getLoginId(),
                    user.getLoginPassword(),
                    user.getEmail(),
                    user.getAddress(),
                    user.getAge(),
                    user.getGender(),
                    user.getBirthDate()
            );
        }
    */
        // 프로필 조회
        public UserProfileResponse getUserProfile(Long userId) {

            User user = userRepository.findById(userId).orElseThrow(
                    ()-> new GeneralException(ErrorCode.USER_NOT_FOUND));

            return  UserProfileResponse.from(user);
        }

        /**
         * 사용자 검색: 입력 텍스트가 이메일 로컬 파트(@ 앞)에 포함되면 매칭.
         * 검색어에 @가 있으면 도메인은 무시하고 @ 앞만 사용한다.
         */
        public List<UserSearchResponse> searchByEmail(String email, Long currentUserId, Long groupId) {

            String localQuery = extractLocalPartForSearch(email);
            if (localQuery == null) {
                return List.of();
            }

            return userRepository.findByEmailLocalPartContaining(localQuery).stream()
                    .filter(u -> !u.getId().equals(currentUserId))
                    .map(u -> {
                        boolean alreadySelected = groupId != null
                                && groupMemberRepository.existsByStudyGroupIdAndUserId(groupId, u.getId());
                        return UserSearchResponse.from(u, alreadySelected);
                    })
                    .toList();
        }

        /** 비교에 쓸 검색어: @ 이후(도메인)는 제거하고 앞부분만 trim */
        private static String extractLocalPartForSearch(String raw) {
            if (raw == null) {
                return null;
            }
            String s = raw.trim();
            int at = s.indexOf('@');
            if (at >= 0) {
                s = s.substring(0, at).trim();
            }
            return s.isEmpty() ? null : s;
        }

        // 사용자의 파일 사용량 조회
        public StorageResponse getStorageUsage(Long userId){

            long selfStudySize = selfStudyRepository.sumFileSizeByUserId(userId);
            long groupDocSize = groupDocumentRepository.sumFileSizeByUploaderId(userId);
            long totalUsed = selfStudySize + groupDocSize;

            return StorageResponse.of(totalUsed);
        }
    }
