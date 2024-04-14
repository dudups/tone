package com.ezone.ezproject.modules.company.service;

import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author cf
 */
class RoleRankCmdServiceTest {

    @Test
    void nextRank() {
    }

    @Test
    void lowerRank() {
        RoleRankCmdService service = new RoleRankCmdService();
        ProjectRole a = ProjectRole.builder().source(RoleSource.CUSTOM).key("a").rank("00010").build();
        ProjectRole b = ProjectRole.builder().source(RoleSource.CUSTOM).key("b").rank("00011").build();
        ProjectRole c = ProjectRole.builder().source(RoleSource.CUSTOM).key("c").rank("00012").build();
        ProjectRole d = ProjectRole.builder().source(RoleSource.CUSTOM).key("d").rank("00013").build();
        ProjectRole f = ProjectRole.builder().source(RoleSource.CUSTOM).key("f").rank(null).build();
        ProjectRole h = ProjectRole.builder().source(RoleSource.CUSTOM).key("h").rank("").build();
        List<ProjectRole> projectRoles = Arrays.asList(b, a,  c, d, f, h);
        for (int i = 0; i < 200; i++) {
            ProjectRoleSchema schema = ProjectRoleSchema.builder().roles(projectRoles).build();
            String result = service.lowerRank(schema, "00012", RoleSource.CUSTOM);
            assertEquals("00011", result);
        }
    }

    @Test
    void lowerRank2() {
        RoleRankCmdService service = new RoleRankCmdService();
        ProjectRole a = ProjectRole.builder().source(RoleSource.CUSTOM).key("a").rank("00010").build();
        ProjectRole b = ProjectRole.builder().source(RoleSource.CUSTOM).key("b").rank("00011").build();
        ProjectRole c = ProjectRole.builder().source(RoleSource.CUSTOM).key("c").rank("00012").build();
        ProjectRole d = ProjectRole.builder().source(RoleSource.CUSTOM).key("d").rank("00013").build();
        ProjectRole f = ProjectRole.builder().source(RoleSource.CUSTOM).key("f").rank(null).build();
        ProjectRole h = ProjectRole.builder().source(RoleSource.CUSTOM).key("h").rank("00014").build();
        List<ProjectRole> projectRoles = Arrays.asList(b, a,  c, d, f, h);
        for (int i = 0; i < 200; i++) {
            ProjectRoleSchema schema = ProjectRoleSchema.builder().roles(projectRoles).build();
            String result = service.lowerRank(schema, "00010", RoleSource.CUSTOM);
            assertEquals(RoleRankCmdService.START_RANK, result);
        }
    }

    @Test
    void higherRank() {
        RoleRankCmdService service = new RoleRankCmdService();
        ProjectRole a = ProjectRole.builder().source(RoleSource.CUSTOM).key("a").rank("00010").build();
        ProjectRole b = ProjectRole.builder().source(RoleSource.CUSTOM).key("b").rank("00011").build();
        ProjectRole c = ProjectRole.builder().source(RoleSource.CUSTOM).key("c").rank("00012").build();
        ProjectRole d = ProjectRole.builder().source(RoleSource.CUSTOM).key("d").rank("00013").build();
        ProjectRole f = ProjectRole.builder().source(RoleSource.CUSTOM).key("f").rank(null).build();
        ProjectRole h = ProjectRole.builder().source(RoleSource.CUSTOM).key("h").rank("").build();
        List<ProjectRole> projectRoles = Arrays.asList(a, b, c, d, f, h);
        for (int i = 0; i < 200; i++) {
            Collections.shuffle(projectRoles);
            ProjectRoleSchema schema = ProjectRoleSchema.builder().maxRank("00013").roles(projectRoles).build();
            String result = service.higherRank(schema, "00011", RoleSource.CUSTOM);
            assertEquals("00012", result);
        }
    }

    @Test
    void higherRank2() {
        RoleRankCmdService service = new RoleRankCmdService();
        ProjectRole a = ProjectRole.builder().source(RoleSource.CUSTOM).key("a").rank("00010").build();
        ProjectRole b = ProjectRole.builder().source(RoleSource.CUSTOM).key("b").rank("00011").build();
        ProjectRole c = ProjectRole.builder().source(RoleSource.CUSTOM).key("c").rank("00012").build();
        ProjectRole d = ProjectRole.builder().source(RoleSource.CUSTOM).key("d").rank("00013").build();
        ProjectRole f = ProjectRole.builder().source(RoleSource.CUSTOM).key("f").rank(null).build();
        ProjectRole h = ProjectRole.builder().source(RoleSource.CUSTOM).key("h").rank("").build();
        List<ProjectRole> projectRoles = Arrays.asList(a, b, c, d, f, h);
        for (int i = 0; i < 200; i++) {
            Collections.shuffle(projectRoles);
            ProjectRoleSchema schema = ProjectRoleSchema.builder().maxRank("00013").roles(projectRoles).build();
            String result = service.higherRank(schema, "00013", RoleSource.CUSTOM);
            assertEquals("00014", result);
        }
    }
}