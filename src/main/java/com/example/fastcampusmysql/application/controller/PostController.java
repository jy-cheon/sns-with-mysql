package com.example.fastcampusmysql.application.controller;

import com.example.fastcampusmysql.application.usecase.CreatePostLikeUsacase;
import com.example.fastcampusmysql.application.usecase.CreatePostUsecase;
import com.example.fastcampusmysql.application.usecase.GetTimelinePostsUsecase;
import com.example.fastcampusmysql.domain.post.dto.DailyPostCount;
import com.example.fastcampusmysql.domain.post.dto.DailyPostCountRequest;
import com.example.fastcampusmysql.domain.post.dto.PostCommand;
import com.example.fastcampusmysql.domain.post.dto.PostDto;
import com.example.fastcampusmysql.domain.post.entity.Post;
import com.example.fastcampusmysql.domain.post.service.PostReadService;
import com.example.fastcampusmysql.domain.post.service.PostWriteService;
import com.example.fastcampusmysql.util.CursorRequest;
import com.example.fastcampusmysql.util.PageCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostWriteService postWriteService;
    private final PostReadService postReadService;
    private final GetTimelinePostsUsecase getTimelinePostsUsecase;
    final private CreatePostUsecase createPostUsecase;
    final private CreatePostLikeUsacase createPostLikeUsacase;

    @PostMapping("/")
    public Long create(PostCommand command) {
//        return postWriteService.create(command);
        return createPostUsecase.execute(command);
    }

    @GetMapping("/daily-post-counts")
    public List<DailyPostCount> getDailyPostCounts(DailyPostCountRequest request) {
        return postReadService.getDailyPostCounts(request);
    }

//    @GetMapping("/member/{memberId}")
//    public Page<Post> getPosts(
//            @PathVariable Long memberId,
//            @RequestParam Integer page,
//            @RequestParam Integer size
//    ) {
//        return postReadService.getPosts(memberId, PageRequest.of(page, size));
//    }
    @GetMapping("/member/{memberId}")
    public Page<PostDto> getPosts(
            @PathVariable Long memberId,
             Pageable pageable
            ) {
        return postReadService.getPosts(memberId, pageable);
    }

    @GetMapping("/members/{memberId}/by-cursor")
    public PageCursor<Post> getPostsByCursor(
            @PathVariable Long memberId,
            CursorRequest cursorRequest
    ) {
        return postReadService.getPosts(memberId, cursorRequest);
    }

    @GetMapping("/members/{memberId}/timeline")
    public PageCursor<Post> getTimeline(
            @PathVariable Long memberId,
            CursorRequest cursorRequest
    ) {
//        return getTimelinePostsUsecase.execute(memberId, cursorRequest);
        return getTimelinePostsUsecase.executeByTimeline(memberId, cursorRequest);
    }

    @PostMapping("/posts/{postId}/like/v1")
    public void likePost(@PathVariable Long postId) {
//        postWriteService.likePost(postId);
        postWriteService.likePostByOptimisticLock(postId);
    }

    @PostMapping("/posts/{postId}/like/v2")
    public void like(
            @PathVariable Long postId,
            @RequestParam Long memberId
    ) {
        createPostLikeUsacase.execute(postId, memberId);
    }


}
