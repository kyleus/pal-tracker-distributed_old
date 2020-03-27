package io.pivotal.pal.tracker.allocations.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

@Repository
public class AllocationDataGateway {

    private JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger( AllocationDataGateway.class );
    public AllocationDataGateway(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    public AllocationRecord create(AllocationFields fields) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "insert into allocations (project_id, user_id, first_day, last_day) values (?, ?, ?, ?)", RETURN_GENERATED_KEYS
            );

            ps.setLong(1, fields.projectId);
            ps.setLong(2, fields.userId);
            ps.setDate(3, Date.valueOf(fields.firstDay));
            ps.setDate(4, Date.valueOf(fields.lastDay));
            return ps;
        }, keyHolder);

        log.warn("Creating Allocation record:\n  projectId: {},\n  userId: {},\n  firstDay: {},\n  lastDay: {}",
                fields.projectId, fields.userId, fields.firstDay, fields.lastDay);
        return find(keyHolder.getKey().longValue());
    }

    public List<AllocationRecord> findAllByProjectId(Long projectId) {
        return jdbcTemplate.query(
            "select id, project_id, user_id, first_day, last_day from allocations where project_id = ? order by first_day",
            rowMapper, projectId
        );
    }


    private AllocationRecord find(long id) {
        return jdbcTemplate.queryForObject(
            "select id, project_id, user_id, first_day, last_day from allocations where id = ?",
            rowMapper, id
        );
    }

    private RowMapper<AllocationRecord> rowMapper =
        (rs, rowNum) -> AllocationRecord.allocationRecordBuilder()
            .id(rs.getLong("id"))
            .projectId(rs.getLong("project_id"))
            .userId(rs.getLong("user_id"))
            .firstDay(rs.getDate("first_day").toLocalDate())
            .lastDay(rs.getDate("last_day").toLocalDate())
            .build();
}
