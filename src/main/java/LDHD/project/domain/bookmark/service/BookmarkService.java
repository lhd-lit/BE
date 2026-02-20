package LDHD.project.domain.bookmark.service;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.domain.bookmark.Bookmark;
import LDHD.project.domain.bookmark.repository.BookmarkRepository;
import LDHD.project.domain.bookmark.web.controller.dto.CreateBookmarkResponse;
import LDHD.project.domain.bookmark.web.controller.dto.DeleteBookmarkResponse;
import LDHD.project.domain.bookmark.web.controller.dto.GetBookmarkListResponse;
import LDHD.project.domain.selfStudy.SelfStudy;
import LDHD.project.domain.selfStudy.repository.SelfStudyRepository;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final SelfStudyRepository selfStudyRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateBookmarkResponse createBookmark(Long selfStudyId, Long currentUserId) {

        // 사용자 확인
        User user = userRepository.findById(currentUserId).orElseThrow(
                () -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 게시물 존재 확인
        SelfStudy selfStudy = selfStudyRepository.findById(selfStudyId).orElseThrow(
                () -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        //즐겨찾기 중복 검사
        if (bookmarkRepository.findByUser_IdAndSelfStudy_Id(currentUserId, selfStudyId).isPresent()) {
            throw new GeneralException(ErrorCode.DUPLICATE_RESOURCE);
        }

        Bookmark bookmark = new Bookmark(user, selfStudy);
        Bookmark savedBookmark = bookmarkRepository.save(bookmark); //DB에 저장

        return new CreateBookmarkResponse(
                savedBookmark.getId(),
                selfStudy.getId(),
                user.getId()
        );
    }
    //즐겨찾기에서 삭제 로직
    @Transactional
    public DeleteBookmarkResponse deleteBookmark(Long selfStudyId, Long currentUserId){

        // 게시물 존재 확인
        selfStudyRepository.findById(selfStudyId).orElseThrow(
                () -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 즐겨찾기 안에 게시물 존재 검사
        Bookmark bookmark = bookmarkRepository.findByUser_IdAndSelfStudy_Id(currentUserId, selfStudyId).orElseThrow(
                () -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        bookmarkRepository.delete(bookmark);

        return new DeleteBookmarkResponse(selfStudyId, currentUserId);
    }

    // 즐겨찾기 목록 조회
    public Page<GetBookmarkListResponse> getBookmarks(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return bookmarkRepository.findByUserIdWithSelfStudy(userId, pageable)
                .map(GetBookmarkListResponse::from);
    }

}
