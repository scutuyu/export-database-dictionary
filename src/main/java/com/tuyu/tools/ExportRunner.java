package com.tuyu.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author tuyu
 * @date 6/9/19
 * Talk is cheap, show me the code.
 */
@Slf4j
@Component
public class ExportRunner implements CommandLineRunner {

    @Autowired
    private ExportSql exportSql;

    @Autowired
    private ExportExcel exportExcel;

    @Override
    public void run(String... strings) throws Exception {
        try {
            exportExcel.export();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("导出数据字典失败", e);
        }

        try {
            exportSql.export();
        } catch (Exception e) {
            log.error("导出sql脚本失败", e);
        }
    }
}
