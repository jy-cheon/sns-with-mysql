package com.example.fastcampusmysql.domain.post.service;

import com.example.fastcampusmysql.domain.post.dto.DailyPostCount;
import com.example.fastcampusmysql.domain.post.dto.PostDto;
import com.example.fastcampusmysql.domain.post.entity.Post;
import com.example.fastcampusmysql.domain.post.repository.PostLikeRepository;
import com.example.fastcampusmysql.domain.post.repository.PostRepository;
import com.example.fastcampusmysql.domain.post.dto.DailyPostCountRequest;
import com.example.fastcampusmysql.util.CursorRequest;
import com.example.fastcampusmysql.util.PageCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PostReadService {
    final private PostRepository postRepository;
    final private PostLikeRepository postLikeRepository;

    public List<DailyPostCount> getDailyPostCounts(DailyPostCountRequest request) {
        return postRepository.groupByCreatedDate(request);
    }

    public Page<PostDto> getPosts(Long memberId, Pageable pageRequest) {
        return postRepository.findAllByMemberId(memberId, pageRequest)
                .map(this::toDto);
    }

    private PostDto toDto(Post post) {
        return new PostDto(
                post.getId(),
                post.getContents(),
                post.getCreatedAt(),
                postLikeRepository.countByPostId(post.getId())
        );
    }

    public PageCursor<Post> getPosts(Long memberId, CursorRequest cursorRequest) {
        var posts = findAllBy(memberId, cursorRequest);
        long nextKey = getNextKey(posts);
        return new PageCursor<>(cursorRequest.next(nextKey), posts);
    }

    // timeline 구현
    public PageCursor<Post> getPosts(List<Long> memberId, CursorRequest cursorRequest) {
        var posts = findAllBy(memberId, cursorRequest);
        long nextKey = getNextKey(posts);
        return new PageCursor<>(cursorRequest.next(nextKey), posts);
    }

    public List<Post> getPosts(List<Long> postIds) {
        return postRepository.findAllByIdIn(postIds);
    }

    public Post getPost(Long postId) {
        return postRepository.findById(postId, false).orElseThrow();
    }

    private List<Post> findAllBy(Long memberId, CursorRequest cursorRequest) {
        if (cursorRequest.hasKey()) {
            return postRepository.findAllByLessThanIdAndMemberIdAndOrderByIdDesc(
                    cursorRequest.key(),
                    memberId,
                    cursorRequest.size());
        }
        return postRepository.findAllByMemberIdAndOrderByIdDesc(memberId, cursorRequest.size());
    }

    // timeline 구현
    private List<Post> findAllBy(List<Long> memberIds, CursorRequest cursorRequest) {
        if (cursorRequest.hasKey()) {
            return postRepository.findAllByLessThanIdAndInMemberIdsAndOrderByIdDesc(
                    cursorRequest.key(),
                    memberIds,
                    cursorRequest.size()
            );
        }
        return postRepository.findAllByInMemberIdsAndOrderByIdDesc(memberIds, cursorRequest.size());
    }

    private static long getNextKey(List<Post> posts) {
        return posts.stream()
                .mapToLong(Post::getId)
                .min()
                .orElse(CursorRequest.NONE_KEY);
    }



//    final private PostLikeRepository postLikeRepository;
//
//    public List<Post> getPosts(Long memberId) {
//        return postRepository.findByMemberId(memberId);
//    }


//
//    public List<PostDto> getPostDtos(List<Long> postIds) {
//        return postRepository.findAllByIdIn(postIds).stream().map(this::toDto).toList();
//    }
//
//    public PageCursor<PostDto> getPostDtos(List<Long> memberIds, CursorRequest cursorRequest) {
//        var posts = findAllBy(memberIds, cursorRequest);
//        long nextKey = getNextKey(posts);
//        var postDtos = posts.stream().map(this::toDto).toList();
//        return new PageCursor<>(cursorRequest.next(nextKey), postDtos);
//    }


//
//    public Page<PostDto> getPostDtos(Long memberId, PageRequest pageRequest) {
//        return postRepository.findAllByMemberId(memberId, pageRequest).map(this::toDto);
//    }
//


//


}