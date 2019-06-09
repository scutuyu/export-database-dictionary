package com.tuyu.tools;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询建表sql，并导出到文件
 *
 * @author tuyu
 * @date 3/29/19
 * Talk is cheap, show me the code.
 */
@Slf4j
@Component
public class ExportSql {

    private static final String EMPTY_STRING = "";

    @Value("${tools.data-dictionary.sql-file-path}")
    @Getter
    @Setter
    private String sqlFilePath;

    @Value("${tools.data-dictionary.database-name}")
    @Getter
    @Setter
    private String databaseName;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 导出建表sql,必要的参数就是文件路径、数据库名，还要配置正确的数据库连接
     *
     * @exception Exception
     */
    public void export() throws Exception{
        FileWriter fw = new FileWriter(new File(sqlFilePath));
        String separator = ";\n\n";
        String dbName = getDatabaseName();
        fw.write(modifyString(getDatabaseSchema(dbName)));
        fw.write(separator);
        List<String> tableNames = getTableNames(dbName);
        String tableSchema = null;
        for (String tn : tableNames) {
            if (StringUtils.isEmpty((tableSchema = getTableSchema(dbName, tn)))) {
                log.error("can not export `{}` sql script...", tn);
                continue;
            }
            fw.write(modifyString(tableSchema));
            fw.write(separator);
            log.info("export {} sql script success!", tn);
        }
        fw.flush();
        fw.close();
        log.info("exported sql file located in: {}\n", getSqlFilePath());
    }

    private String getDatabaseSchema(String dbName) {
        StringBuilder sql = new StringBuilder("show create database ");
        sql.append(dbName);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql.toString());
        if (list.size() > 0) {
            Object r = null;
            return (r = list.get(0).get("Create Database")) == null ? EMPTY_STRING : r.toString();
        }
        return EMPTY_STRING;
    }

    private String getTableSchema(String dbName, String tableName) {
        String sql = "show create table `%s`.`%s`";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(String.format(sql, dbName, tableName));
        if (list.size() > 0) {
            Map<String, Object> map = list.get(0);
            Object value = map.get("Create Table");
            return value == null ? EMPTY_STRING : value.toString();
        }
        return EMPTY_STRING;
    }

    /**
     * 查询所有表名
     *
     * @param dbName
     *
     * @return
     */
    private List<String> getTableNames(String dbName) {
        String sql = "select table_name from information_schema.tables where table_schema=?";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, dbName);
        List<String> re = new ArrayList<>(list.size());
        Object value = null;
        for (Map<String, Object> map : list) {
            if ((value = map.get("table_name")) != null) {
                re.add(value.toString());
            }
        }
        return re;
    }

    /**
     * 对建表sql进行修饰
     *
     * @param string
     *
     * @return
     */
    public static final String modifyString(String string) {
        if (StringUtils.isEmpty(string)) {
            return string;
        }
        // 不区分大小写 (?i)
        // 0个或多个数字 \\d*
        // 0个货1个空格  ?
        return string.replaceAll("(?i)auto_increment=\\d* ?", EMPTY_STRING);
    }

}
