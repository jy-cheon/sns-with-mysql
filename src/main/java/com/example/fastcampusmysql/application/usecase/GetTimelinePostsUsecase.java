package com.example.fastcampusmysql.application.usecase;

import com.example.fastcampusmysql.domain.follow.entity.Follow;
import com.example.fastcampusmysql.domain.follow.service.FollowReadService;
import com.example.fastcampusmysql.domain.post.entity.Post;
import com.example.fastcampusmysql.domain.post.entity.Timeline;
import com.example.fastcampusmysql.domain.post.service.PostReadService;
import com.example.fastcampusmysql.domain.post.service.TimelineReadService;
import com.example.fastcampusmysql.util.CursorRequest;
import com.example.fastcampusmysql.util.PageCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GetTimelinePostsUsecase {
    final private FollowReadService followReadService;
    final private PostReadService postReadService;
    final private TimelineReadService timelineReadService;

    public PageCursor<Post> execute(Long memberId, CursorRequest cursorRequest) {
        /*
        * 1. memberId -> follow 조회
        * 2. 1번 결과로 게시물 조회
        * */

        var follows = followReadService.getFollowings(memberId);
        var followMemberIds = follows
                .stream()
                .map( Follow::getToMemberId)
                .toList();

        return postReadService.getPosts(followMemberIds, cursorRequest);
    }

    public PageCursor<Post> executeByTimeline(Long memberId, CursorRequest cursorRequest) {
        /*
         * 1. Timeline 조회
         * 2. 1번에 해당하는 게시물을 조회한다.
         * */

        var pagedTimelines = timelineReadService.getTimelines(memberId, cursorRequest);
        var postIds = pagedTimelines.body().stream().map(Timeline::getPostId).toList();
        var posts = postReadService.getPosts(postIds);

        return new PageCursor<>(pagedTimelines.nextCursorRequest(), posts);
    }





}
