package com.tuyu.tools;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 读取数据字典，并导出到excel文件中
 *
 * @author tuyu
 * @date 3/28/19
 * Talk is cheap, show me the code.
 */
@Slf4j
@Component
public class ExportExcel {

    private static final   String[] columns = "字段,数据类型,主键,注释".split(",");

    @Value("${tools.data-dictionary.file-path}")
    @Getter
    @Setter
    private String filePath;

    @Value("${tools.data-dictionary.database-name}")
    @Getter
    @Setter
    private String databaseName;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 导出数据字典,必要的参数就是文件路径、数据库名，还要配置正确的数据库连接
     *
     * @exception Exception
     */
    public void export() throws Exception {
        List<Map<String, Object>> tables = getTableNames(getDatabaseName());
        Workbook workbook = new XSSFWorkbook();
        setOverview(workbook, tables);
        Object tableName = null;
        Object tableComment = null;
        for (Map<String, Object> map : tables) {
            tableName = map.get("table_name");
            tableComment = map.get("table_comment");
            if (StringUtils.isEmpty(tableName) || StringUtils.isEmpty(tableComment)) {
                log.error("can not export table : {}, table comment is not full...", tableName);
                continue;
            }
            setTable(workbook, getDatabaseName(), getString(tableName), getString(tableComment));
            log.info("export table : {} success!", tableName);
        }
        FileOutputStream fileOutputStream = new FileOutputStream(getFilePath());
        workbook.write(fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
        workbook.close();
        log.info("exported dictionary file located in: {}\n", getFilePath());
    }
    private List<Map<String, Object>> getTableNames(String databaseName) {
        String sql = "select table_name, table_comment from information_schema.tables where table_schema='%s'";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(String.format(sql, databaseName));
        return list;
    }

    private List<Map<String, Object>> getTableStructure(String databaseName, String tableName) {
        StringBuilder stringBuilder = new StringBuilder("SELECT column_name '%s', ");
        stringBuilder.append("column_type '%s', CASE column_key WHEN 'PRI' THEN 'true' ELSE 'false' END AS '%s', ")
                .append("column_comment '%s' FROM information_schema.COLUMNS ")
                .append("WHERE table_schema = '%s' AND table_name = '%s'");
        List<String> params = new ArrayList<>(columns.length);
        params.addAll(Arrays.asList(columns));
        params.add(databaseName);
        params.add(tableName);
        String sql = String.format(stringBuilder.toString(), params.toArray());
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        return list;
    }

    private void setOverview(Workbook workbook, String databaseName) {
        List<Map<String, Object>> tables = getTableNames(databaseName);
        setOverview(workbook, tables);
    }

    private void setOverview(Workbook workbook, List<Map<String, Object>> tables){
        Sheet sheet = workbook.createSheet("总览");
        int i = 0;
        for (Map<String, Object> map : tables) {
            Row row = sheet.createRow(i++);
            Cell cell = row.createCell(0);
            cell.setCellValue(map.get("table_name").toString());
            Object tableName = map.get("table_comment");
            if (tableName != null) {
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(tableName.toString());
            }
        }
    }

    private void setTable(Workbook workbook, String datableName, String tableName, String tableComment) {
        Sheet sheet = workbook.createSheet(tableComment);
        Row firstRow = sheet.createRow(0);
        Cell firstCell = firstRow.createCell(0);
        firstCell.setCellValue(tableName);
        Row header = sheet.createRow(1);
        int i = 0;
        for (String column : columns) {
            Cell cell = header.createCell(i++);
            cell.setCellValue(column);
        }
        int j = 2;
        List<Map<String, Object>> list = getTableStructure(datableName, tableName);
        for (Map<String, Object> map : list) {
            Row row = sheet.createRow(j++);
            int k = 0;
            for (String column : columns) {
                Object o = map.get(column);
                Cell cell = row.createCell(k++);
                cell.setCellValue(getString(o));
            }
        }
    }

    private String getString(Object object) {
        if (object == null) {
            return "";
        }
        return object.toString();
    }
}
