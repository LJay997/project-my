package com.qq.ijay997.excel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.qq.ijay997.masking.Sensitive;
import com.qq.ijay997.masking.SensitiveValues;

/**
 * Excel 导出通用脱敏处理器。
 * <p>
 * 在单元格数据完成类型转换后、写入 POI 之前介入：
 * 读取当前列表头对应字段上的 {@link Sensitive} 注解，无权限时把单元格字符串替换为脱敏值。
 * <p>
 * 与 {@code SensitiveDataMasker} 的区别：只改 {@link WriteCellData}，不反射修改源对象，
 * 因此同一份数据可以先导出、再用于 JSON 响应，互不污染。
 * <p>
 * 通过 {@link MaskingExcelWriter} 创建写出器即可自动挂载本处理器。
 *
 * @author ijay997
 */
public class SensitiveExcelWriteHandler implements CellWriteHandler {

    @Override
    public void afterCellDataConverted(CellWriteHandlerContext context) {
        if (Boolean.TRUE.equals(context.getHead())) {
            return;
        }
        Head head = context.getHeadData();
        if (head == null || head.getField() == null) {
            return;
        }
        Sensitive sensitive = head.getField().getAnnotation(Sensitive.class);
        if (sensitive == null) {
            return;
        }
        WriteCellData<?> cellData = context.getFirstCellData();
        if (cellData == null || cellData.getStringValue() == null) {
            return;
        }
        cellData.setStringValue(
                SensitiveValues.maskIfNeeded(sensitive, cellData.getStringValue()));
    }
}
