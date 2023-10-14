package com.example.fastcampusmysql.domain.post.repository;

import com.example.fastcampusmysql.util.PageHelper;
import com.example.fastcampusmysql.domain.post.dto.DailyPostCount;
import com.example.fastcampusmysql.domain.post.dto.DailyPostCountRequest;
import com.example.fastcampusmysql.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class PostRepository {
    static final String TABLE = "Post";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final RowMapper<DailyPostCount> DAILY_POST_COUNT_MAPPER = (ResultSet resultSet, int rowNum)
            -> new DailyPostCount(
                resultSet.getLong("memberId"),
                resultSet.getObject("createdDate", LocalDate.class),
                resultSet.getLong("cnt")
        );


    private static final RowMapper<Post> ROW_MAPPER = (ResultSet resultSet, int rowNum) -> Post.builder()
            .id(resultSet.getLong("id"))
            .memberId(resultSet.getLong("memberId"))
            .contents(resultSet.getString("contents"))
            .createdDate(resultSet.getObject("createdDate", LocalDate.class))
            .createdAt(resultSet.getObject("createdAt", LocalDateTime.class))
            .likeCount(resultSet.getLong("likeCount"))
            .version(resultSet.getLong("version"))
            .build();

    public Post save(Post post) {
        if (post.getId() == null)
            return insert(post);
        return update(post);
//        throw new UnsupportedOperationException("Post는 갱신을 지원하지 않습니다.");
    }

    private Post insert(Post post) {
        SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
                .withTableName(TABLE)
                .usingGeneratedKeyColumns("id");

        SqlParameterSource params = new BeanPropertySqlParameterSource(post);
        var id = jdbcInsert.executeAndReturnKey(params).longValue();

        return Post.builder()
                .id(id)
                .memberId(post.getMemberId())
                .contents(post.getContents())
                .createdDate(post.getCreatedDate())
                .createdAt(post.getCreatedAt())
                .build();
    }

    private Post update(Post post) {
        var sql = String.format("""
        UPDATE %s set
            memberId = :memberId,
            contents = :contents,
            createdDate = :createdDate,
            createdAt = :createdAt,
            likeCount = :likeCount,
            version = :version + 1
        WHERE id = :id and version = :version
        """, TABLE);

        SqlParameterSource params = new BeanPropertySqlParameterSource(post);
        var updatedCount = namedParameterJdbcTemplate.update(sql, params);
        if (updatedCount == 0) {
            throw new RuntimeException("not updated");
        }
        return post;
    }

    private Post update1(Post post) {
        var sql = String.format("""

        UPDATE %s set
            memberId = :memberId,
            contents = :contents,
            createdDate = :createdDate,
            createdAt = :createdAt,
            likeCount = :likeCount
        WHERE id = :id 
        """, TABLE);

//        UPDATE %s set
//        memberId = :memberId,
//                contents = :contents,
//                createdDate = :createdDate,
//                createdAt = :createdAt,
//                likeCount = :likeCount,
//                version = :version + 1
//        WHERE id = :id and version = :version

        SqlParameterSource params = new BeanPropertySqlParameterSource(post);
        var updatedCount = namedParameterJdbcTemplate.update(sql, params);
        if (updatedCount == 0) {
            throw new RuntimeException("not updated");
        }
        return post;
    }

//    public List<Post> findByMemberId(Long memberId) {
//        var params = new MapSqlParameterSource()
//                .addValue("memberId", memberId);
//        String query = String.format("SELECT * FROM `%s` WHERE id = :id", TABLE);
//        return namedParameterJdbcTemplate.query(query, params, ROW_MAPPER);
//    }

    public Optional<Post> findById(Long postId, boolean requiredLock) {
        String query = String.format("""
                SELECT *
                FROM %s
                WHERE id = :postId
                """, TABLE);
        if (requiredLock) {
            query += "FOR UPDATE";
        }

        var params = new MapSqlParameterSource()
                .addValue("postId", postId);
        var nullablePost = namedParameterJdbcTemplate.queryForObject(query, params, ROW_MAPPER);
        return Optional.ofNullable(nullablePost);
    }

    public List<DailyPostCount> groupByCreatedDate(DailyPostCountRequest request) {
        String query = String.format("""
                SELECT memberId, createdDate, count(id) as cnt
                FROM %s
                WHERE memberId = :memberId and createdDate between :firstDate and :lastDate
                GROUP BY memberId, createdDate
                """, TABLE);
        var params = new BeanPropertySqlParameterSource(request);

        return namedParameterJdbcTemplate.query(query, params, DAILY_POST_COUNT_MAPPER);
    }

    public Page<Post> findAllByMemberId(Long memberId, Pageable pageable) {
        var params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("offset", pageable.getOffset())
                .addValue("size", pageable.getPageSize());

        Sort sort = pageable.getSort();
        String query1 = String.format("""
                SELECT *
                FROM %s
                WHERE memberId = :memberId
                LIMIT :offset, :size
                """, TABLE);

        String query = String.format("""
                SELECT *
                FROM %s
                WHERE memberId = :memberId
                ORDER BY %s
                LIMIT :offset, :size
                """, TABLE, PageHelper.orderBy(sort));

        var posts = namedParameterJdbcTemplate.query(query, params, ROW_MAPPER);
        return new PageImpl<Post>(posts, pageable, getCount(memberId));
    }

    private Long getCount(Long memberId) {
        String countQuery = String.format("""
                SELECT count(id)
                FROM %s
                WHERE memberId = :memberId
                """, TABLE);
        var countParam = new MapSqlParameterSource().addValue("memberId", memberId);
        return namedParameterJdbcTemplate.queryForObject(countQuery,  countParam, Long.class);
    }

    // timeline push 구현
    public List<Post> findAllByIdIn(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        String query = String.format("""
                SELECT *
                FROM %s
                WHERE id in (:postIds)
                """, TABLE);

        var params = new MapSqlParameterSource()
                .addValue("postIds", postIds);

        return namedParameterJdbcTemplate.query(query, params, ROW_MAPPER);
    }

    public List<Post> findAllByMemberIdAndOrderByIdDesc(Long memberId, int size) {
        String query = String.format("""
            SELECT *
            FROM %s
            WHERE memberId = :memberId
            ORDER BY id DESC
            LIMIT :size
            """, TABLE);

        var params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("size", size);

        return namedParameterJdbcTemplate.query(query, params, ROW_MAPPER);
    }

    public List<Post> findAllByLessThanIdAndMemberIdAndOrderByIdDesc(Long id, Long memberId, int size) {
        String query = String.format("""
            SELECT *
            FROM %s
            WHERE memberId = :memberId and id < :id
            ORDER BY id DESC
            LIMIT :size
            """, TABLE);

        var params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("id", id)
                .addValue("size", size);

        return namedParameterJdbcTemplate.query(query, params, ROW_MAPPER);
    }

    // timeline service 에서 이용
    public List<Post> findAllByInMemberIdsAndOrderByIdDesc(List<Long> memberIds, int size) {
        if (memberIds.isEmpty()) {
            return List.of();
        }

        var params = new MapSqlParameterSource()
                .addValue("memberIds", memberIds)
                .addValue("size", size);

        String query = String.format("""
                SELECT *
                FROM %s
                WHERE memberId in (:memberIds)
                ORDER BY id DESC
                LIMIT :size
                """, TABLE);

        return namedParameterJdbcTemplate.query(query, params, ROW_MAPPER);
    }

    // timeline service 에서 이용
    public List<Post> findAllByLessThanIdAndInMemberIdsAndOrderByIdDesc(Long id, List<Long> memberIds, int size) {
        if (memberIds.isEmpty()) {
            return List.of();
        }

        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("memberIds", memberIds)
                .addValue("size", size);

        String query = String.format("""
                SELECT *
                FROM %s
                WHERE memberId in (:memberIds) and id < :id
                ORDER BY id DESC
                LIMIT :size
                """, TABLE);

        return namedParameterJdbcTemplate.query(query, params, ROW_MAPPER);
    }

    public void bulkInsert(List<Post> posts) {
        var sql = String.format("""
                INSERT INTO `%s` (memberId, contents, createdDate, createdAt)
                VALUES (:memberId, :contents, :createdDate, :createdAt)
                """, TABLE);

        SqlParameterSource[] params = posts
                .stream()
                .map(BeanPropertySqlParameterSource::new)
                .toArray(SqlParameterSource[]::new);
        namedParameterJdbcTemplate.batchUpdate(sql, params);
    }


//
//    public List<Post> findAllByLessThanIdAndMemberIdAndOrderByIdDesc(Long id, Long memberId, int size) {
//        var params = new MapSqlParameterSource()
//                .addValue("id", id)
//                .addValue("memberId", memberId)
//                .addValue("size", size);
//
//        String query = String.format("""
//                SELECT *
//                FROM %s
//                WHERE memberId = :memberId and id < :id
//                ORDER BY id DESC
//                LIMIT :size
//                """, TABLE);
//
//        return namedParameterJdbcTemplate.query(query, params, ROW_MAPPER);
//    }
//
//    public List<Post> findAllByMemberIdAndOrderByIdDesc(Long memberId, int size) {
//        var params = new MapSqlParameterSource()
//                .addValue("memberId", memberId)
//                .addValue("size", size);
//
//        String query = String.format("""
//                SELECT *
//                FROM %s
//                WHERE memberId = :memberId
//                ORDER BY id DESC
//                LIMIT :size
//                """, TABLE);
//
//        return namedParameterJdbcTemplate.query(query, params, ROW_MAPPER);
//    }
//







}