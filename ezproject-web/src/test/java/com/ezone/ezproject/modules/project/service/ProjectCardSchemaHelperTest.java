package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.enums.FieldType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ProjectCardSchemaHelperTest {
    @Test
    public void testNewProjectCardSchema() {
        ProjectCardSchema schema = new ProjectCardSchemaHelper().newSysProjectCardSchema();
        Assert.assertTrue(schema.getFields().size() > 0);
        Assert.assertNotNull(schema.getFields().get(0).getKey());
        Assert.assertTrue(schema.getStatuses().size() > 0);
        Assert.assertNotNull(schema.getStatuses().get(0).getKey());
        List<CardType> types = schema.getTypes();
        Assert.assertEquals(6, types.size());
        Assert.assertNotNull(types.get(0).getKey());
        Assert.assertTrue(types.get(0).getFields().size() > 0);
        Assert.assertTrue(types.get(0).getStatuses().size() > 0);
        Assert.assertNotNull(types.get(0).getStatuses().get(0).getKey());
    }

    @Test
    public void testCardSchemaFieldsMappingYamlFile() {
        List<CardField> fields = Arrays.asList(
                CardField.builder().key("title").valueType(FieldType.ValueType.LONG).build(),
                CardField.builder().key("create_time").valueType(FieldType.ValueType.DATE).build());
        String yaml = VelocityTemplate.render("fields", fields, "/es/card-mapping.yaml");
        Assert.assertTrue(yaml.contains("properties:"));
        Assert.assertTrue(yaml.contains("  title:"));
        Assert.assertTrue(yaml.contains("    type: long"));
        Assert.assertTrue(yaml.contains("  create_time:"));
        Assert.assertTrue(yaml.contains("    type: date"));
        Assert.assertTrue(yaml.contains("    format: epoch_millis"));
    }
}
