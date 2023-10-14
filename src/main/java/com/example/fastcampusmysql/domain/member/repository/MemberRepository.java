package com.example.fastcampusmysql.domain.member.repository;

import com.example.fastcampusmysql.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.support.DataAccessUtils;
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
public class MemberRepository {
    final private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static final String TABLE = "member";

    private static final RowMapper<Member> rowMapper = (ResultSet resultset, int rowNum) -> Member
            .builder()
            .id(resultset.getLong("id"))
            .email(resultset.getString("email"))
            .nickname(resultset.getString("nickname"))
            .birthday(resultset.getObject("birthday", LocalDate.class))
            .createdAt(resultset.getObject("createdAt", LocalDateTime.class))
            .build();


    public Optional<Member> findById(Long id) {
        var sql = String.format("SELECT * FROM %s WHERE id = :id", TABLE);
        var param = new MapSqlParameterSource().addValue("id", id);

        var member = namedParameterJdbcTemplate.queryForObject(sql, param, rowMapper);
        return Optional.ofNullable(member);
    }

    public List<Member> findAllByIdIn(List<Long> ids) {
        var sql = String.format("SELECT * FROM %s WHERE id in (:ids)", TABLE);
        var params = new MapSqlParameterSource().addValue("ids", ids);
        return namedParameterJdbcTemplate.query(sql, params, rowMapper);
    }

//
//    public Optional<Member> findById(Long id) {
//        var sql = String.format("SELECT * FROM %s WHERE id = :id ", TABLE);
//        var params = new MapSqlParameterSource()
//                .addValue("id", id);
//        List<Member> members = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
//
//        // jdbcTemplate.query의 결과 사이즈가 0이면 null, 2 이상이면 예외
//        Member nullableMember = DataAccessUtils.singleResult(members);
//        return Optional.ofNullable(nullableMember);
//    }

    public Member save(Member member) {
        /*
        * member id를 보고 갱신 또는 삽입을 정함
        * 반환값은 id를 담아서 반환한다.
        * */
        if (member.getId() == null) {
            return insert(member);
        }
        return update(member);
    }
    private Member insert(Member member) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
                .withTableName("Member")
                .usingGeneratedKeyColumns("id");
        SqlParameterSource params = new BeanPropertySqlParameterSource(member);
        var id =  simpleJdbcInsert.executeAndReturnKey(params).longValue();

        return Member.builder()
                .id(id)
                .email(member.getEmail())
                .nickname(member.getNickname())
                .birthday(member.getBirthday())
                .createdAt(member.getCreatedAt())
                .build();

    }
    private Member update(Member member) {
        var sql = String.format("UPDATE %s set email = :email, nickname = :nickname, birthday = :birthday WHERE id = :id", TABLE);
        SqlParameterSource params = new BeanPropertySqlParameterSource(member);
        namedParameterJdbcTemplate.update(sql, params);
        return member;
    }

}
