package com.smartmall;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartmall.audit.mapper.AuditLogMapper;
import com.smartmall.ai.mapper.*;
import com.smartmall.catalog.mapper.*;
import com.smartmall.marketing.mapper.*;
import com.smartmall.order.mapper.*;
import com.smartmall.pay.mapper.PaymentMapper;
import com.smartmall.user.mapper.UserAccountMapper;
import com.smartmall.user.mapper.UserAddressMapper;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * 沿用 test/resources/application.yml，直接核验真实 MySQL。
 * 只执行 SELECT；不插入、修改、删除数据，不输出用户资料。
 * 字段覆盖测试有意报告可空但未映射的列，避免将“查询成功”等同于“实体完整”。
 */
@SpringBootTest
class DatabaseMappingVerificationTest {

    private static final int SAMPLE_SIZE = 100;
    private static final List<Class<? extends BaseMapper>> MAPPERS = List.of(
            AuditLogMapper.class, BrandMapper.class, CategoryMapper.class,
            ProductMapper.class, SkuMapper.class, CouponTemplateMapper.class,
            FullReductionMapper.class, SeckillMapper.class, UserCouponMapper.class,
            AfterSaleMapper.class, CartItemMapper.class, FavoriteMapper.class,
            OrderMapper.class, OrderItemMapper.class, OrderLogMapper.class,
            ReviewMapper.class, PaymentMapper.class,
            UserAccountMapper.class, UserAddressMapper.class,
            ChatFeedbackMapper.class, ChatMessageMapper.class, ChatSessionMapper.class,
            KnowledgeDocMapper.class, KnowledgeChunkMapper.class);

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    void connectsToRealMysql() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).isEqualTo("smart_mall");
            System.out.println("DATABASE: MySQL / " + connection.getCatalog());
        }
    }

    @TestFactory
    List<DynamicTest> liveColumnsMatchCurrentSchema() throws Exception {
        // 此解析器只针对 CurrentSchema.sql 的 CREATE TABLE 格式，不解析通用 SQL。
        String schema = Files.readString(Path.of("sql", "CurrentSchema.sql"));
        var tables = Pattern.compile("(?is)create\\s+table\\s+(\\w+)\\s*\\((.*?)\\)\\s*comment")
                .matcher(schema);
        List<DynamicTest> tests = new ArrayList<>();
        while (tables.find()) {
            String table = tables.group(1);
            Set<String> expected = new LinkedHashSet<>();
            var columns = Pattern.compile(
                    "(?im)^\\s*(\\w+)\\s+(?:bigint|int|varchar|text|longtext|datetime|decimal|bit)\\b")
                    .matcher(tables.group(2));
            while (columns.find()) {
                expected.add(columns.group(1));
            }
            tests.add(dynamicTest("SCHEMA " + table, () ->
                    assertThat(databaseColumns(table)).as(table + " actual columns")
                            .containsExactlyInAnyOrderElementsOf(expected)));
        }
        assertThat(tests).as("CREATE TABLE definitions parsed from CurrentSchema.sql").isNotEmpty();
        return tests;
    }

    @TestFactory
    List<DynamicTest> entityMappingsAndExistingData() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Class<? extends BaseMapper> mapperType : MAPPERS) {
            Class<?> entity = ResolvableType.forClass(mapperType)
                    .as(BaseMapper.class).getGeneric(0).resolve();
            TableInfo info = TableInfoHelper.getTableInfo(entity);
            assertThat(info).as(mapperType.getSimpleName() + " table metadata").isNotNull();
            String table = info.getTableName();
            Set<String> mapped = new LinkedHashSet<>();
            mapped.add(info.getKeyColumn());
            info.getFieldList().forEach(field -> mapped.add(field.getColumn()));

            tests.add(dynamicTest("MAPPED_COLUMNS " + table, () ->
                    assertThat(databaseColumns(table)).as(table + " entity columns")
                            .containsAll(mapped)));
            tests.add(dynamicTest("COLUMN_COVERAGE " + table, () ->
                    assertThat(mapped).as(table + " unmapped database columns")
                            .containsAll(databaseColumns(table))));
            tests.add(dynamicTest("READ_EXISTING " + table, () -> readExisting(mapperType, info)));

            for (var field : info.getFieldList()) {
                if (!field.getPropertyType().isEnum()) {
                    continue;
                }
                Set<String> accepted = new LinkedHashSet<>();
                Arrays.stream(field.getPropertyType().getEnumConstants())
                        .forEach(value -> accepted.add(((Enum<?>) value).name()));
                tests.add(dynamicTest("ENUM_VALUES " + table + "." + field.getColumn(), () -> {
                    // 包含逻辑删除行，以检查整个数据库的历史枚举存值。
                    List<String> actual = jdbc.queryForList(
                            "select distinct " + quoted(field.getColumn()) + " from " + quoted(table)
                                    + " where " + quoted(field.getColumn()) + " is not null", String.class);
                    System.out.println("ENUM: " + table + "." + field.getColumn() + " = " + actual);
                    assertThat(accepted).as(table + "." + field.getColumn() + " accepted enum names")
                            .containsAll(actual);
                }));
            }
        }
        return tests;
    }

    @TestFactory
    List<DynamicTest> xmlResultPropertiesExist() {
        List<DynamicTest> tests = new ArrayList<>();
        var configuration = sqlSessionFactory.getConfiguration();
        configuration.getResultMapNames().stream()
                .filter(name -> name.startsWith("com.smartmall.") && name.endsWith(".BaseResultMap"))
                .sorted()
                .forEach(name -> tests.add(dynamicTest("XML_RESULT_MAP " + name, () -> {
                    var resultMap = configuration.getResultMap(name);
                    MetaClass meta = MetaClass.forClass(resultMap.getType(), new DefaultReflectorFactory());
                    for (var mapping : resultMap.getResultMappings()) {
                        assertThat(meta.hasSetter(mapping.getProperty()))
                                .as(name + " missing property: " + mapping.getProperty()).isTrue();
                    }
                })));
        assertThat(tests).as("XML BaseResultMap definitions loaded").isNotEmpty();
        return tests;
    }

    private List<String> databaseColumns(String table) {
        List<String> columns = jdbc.queryForList(
                "select COLUMN_NAME from information_schema.COLUMNS"
                        + " where TABLE_SCHEMA = database() and TABLE_NAME = ? order by ORDINAL_POSITION",
                String.class, table);
        assertThat(columns).as("Table exists: " + table).isNotEmpty();
        return columns;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void readExisting(Class<? extends BaseMapper> mapperType, TableInfo info) {
        BaseMapper mapper = context.getBean(mapperType);
        QueryWrapper<Object> wrapper = new QueryWrapper<Object>().orderByAsc(info.getKeyColumn());
        Page<?> sample = (Page<?>) mapper.selectPage(new Page<>(1, SAMPLE_SIZE), wrapper);
        long expected = jdbc.queryForObject(
                "select count(*) from " + quoted(info.getTableName()) + " where deleted = 0", Long.class);
        assertThat(sample.getTotal()).as(info.getTableName() + " active row count").isEqualTo(expected);
        assertThat(sample.getRecords()).hasSize((int) Math.min(expected, SAMPLE_SIZE));
        System.out.println("READ: " + info.getTableName() + " active=" + expected
                + " sampled=" + sample.getRecords().size());
    }

    private static String quoted(String identifier) {
        // SQL 标识符仅来自实体元数据；仍检查格式，值则通过 JDBC 参数传入。
        if (!identifier.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid metadata identifier");
        }
        return "`" + identifier + "`";
    }
}
