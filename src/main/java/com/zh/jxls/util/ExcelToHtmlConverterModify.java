package com.zh.jxls.util;

import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.converter.AbstractExcelConverter;
import org.apache.poi.hssf.converter.ExcelToHtmlConverter;
import org.apache.poi.hssf.converter.ExcelToHtmlUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.hwpf.converter.HtmlDocumentFacade;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Beta;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 修改poi中的ExcelToHtmlConverter类
 * 对生成HTML相关格式的修改
 * @author RunningHong
 * @date 2018/12/10 9:44
 * @param
 * @return
 */
@Beta
public class ExcelToHtmlConverterModify extends AbstractExcelConverter {

    private static final POILogger logger = POILogFactory.getLogger(ExcelToHtmlConverterModify.class);


    /**
     * Converts Excel file (97-2007) into HTML file.
     *
     * @param xlsFile file to process
     * @return DOM representation of result HTML
     */
    public static Document process(File xlsFile) throws Exception {
        final HSSFWorkbook workbook = ExcelToHtmlUtils.loadXls(xlsFile);
        ExcelToHtmlConverter excelToHtmlConverter = new ExcelToHtmlConverter(XMLHelper.getDocumentBuilderFactory()
                                                                            .newDocumentBuilder().newDocument());
        excelToHtmlConverter.processWorkbook(workbook);
        Document doc = excelToHtmlConverter.getDocument();
        workbook.close();
        return doc;
    }

    private String cssClassContainerCell = null;

    private String cssClassContainerDiv = null;

    private String cssClassPrefixCell = "c";

    private String cssClassPrefixDiv = "d";

    private String cssClassPrefixRow = "r";

    private String cssClassPrefixTable = "t";

    private Map<Short, String> excelStyleToClass = new LinkedHashMap<Short, String>();

    private final HtmlDocumentFacade htmlDocumentFacade;

    private boolean useDivsToSpan = false;

    public ExcelToHtmlConverterModify(Document doc) {
        htmlDocumentFacade = new HtmlDocumentFacade(doc);
    }

    public ExcelToHtmlConverterModify(HtmlDocumentFacade htmlDocumentFacade) {
        this.htmlDocumentFacade = htmlDocumentFacade;
    }

    protected String buildStyle(HSSFWorkbook workbook, HSSFCellStyle cellStyle) {
        StringBuilder style = new StringBuilder();

        style.append("white-space:pre-wrap;");
        ExcelToHtmlUtils.appendAlign(style, cellStyle.getAlignment());

        // 背景色设置
        switch (cellStyle.getFillPattern()) {
            // no fill
            case NO_FILL:
                break;
            case SOLID_FOREGROUND:
                final HSSFColor foregroundColor = cellStyle.getFillForegroundColorColor();
                if (foregroundColor == null) break;
                String fgCol = ExcelToHtmlUtils.getColor(foregroundColor);
                style.append("background-color:").append(fgCol).append(";");
                break;
            default:
                final HSSFColor backgroundColor = cellStyle.getFillBackgroundColorColor();
                if (backgroundColor == null) break;
                String bgCol = ExcelToHtmlUtils.getColor(backgroundColor);
                style.append("background-color:").append(bgCol).append(";");
                break;
        }

        // 设置边框
        buildStyle_border(workbook, style, "top", cellStyle.getBorderTop(), cellStyle.getTopBorderColor());
        buildStyle_border(workbook, style, "right", cellStyle.getBorderRight(), cellStyle.getRightBorderColor());
        buildStyle_border(workbook, style, "bottom", cellStyle.getBorderBottom(), cellStyle.getBottomBorderColor());
        buildStyle_border(workbook, style, "left", cellStyle.getBorderLeft(), cellStyle.getLeftBorderColor());

        // 设置字体
        HSSFFont font = cellStyle.getFont(workbook);
        buildStyle_font(workbook, style, font);

        return style.toString();
    }

    private void buildStyle_border(HSSFWorkbook workbook, StringBuilder style,
                                   String type, BorderStyle xlsBorder, short borderColor) {
        if (xlsBorder == BorderStyle.NONE) {
            return;
        }

        StringBuilder borderStyle = new StringBuilder();
        borderStyle.append(ExcelToHtmlUtils.getBorderWidth(xlsBorder));
        borderStyle.append(' ');
        borderStyle.append(ExcelToHtmlUtils.getBorderStyle(xlsBorder));

        final HSSFColor color = workbook.getCustomPalette().getColor(borderColor);
        if (color != null) {
            borderStyle.append(' ');
            borderStyle.append(ExcelToHtmlUtils.getColor(color));
        }

        style.append("border-" + type + ":" + borderStyle + ";");
    }

    /**
     * 字体设置
     * @Author RunningHong
     * @Date 2018/12/10 21:15
     * @Param
     * @return
     */
    void buildStyle_font(HSSFWorkbook workbook, StringBuilder style, HSSFFont font) {
        if (font.getBold()) {
            style.append("font-weight:bold;");
        }

        final HSSFColor fontColor = workbook.getCustomPalette().getColor(
                font.getColor());
        if (fontColor != null)
            style.append("color: " + ExcelToHtmlUtils.getColor(fontColor)
                    + "; ");

        if (font.getFontHeightInPoints() != 0)
            style.append("font-size:" + font.getFontHeightInPoints() + "pt;");

        if (font.getItalic()) {
            style.append("font-style:italic;");
        }
    }

    public String getCssClassPrefixCell() {
        return cssClassPrefixCell;
    }

    public String getCssClassPrefixDiv() {
        return cssClassPrefixDiv;
    }

    public String getCssClassPrefixRow() {
        return cssClassPrefixRow;
    }

    public String getCssClassPrefixTable() {
        return cssClassPrefixTable;
    }

    public Document getDocument() {
        return htmlDocumentFacade.getDocument();
    }

    protected String getStyleClassName(HSSFWorkbook workbook, HSSFCellStyle cellStyle) {
        final Short cellStyleKey = Short.valueOf(cellStyle.getIndex());

        String knownClass = excelStyleToClass.get(cellStyleKey);
        if (knownClass != null)
            return knownClass;

        String cssStyle = buildStyle(workbook, cellStyle);
        String cssClass = htmlDocumentFacade.getOrCreateCssClass(
                cssClassPrefixCell, cssStyle);
        excelStyleToClass.put(cellStyleKey, cssClass);
        return cssClass;
    }

    public boolean isUseDivsToSpan() {
        return useDivsToSpan;
    }

    protected boolean processCell(HSSFCell cell, Element tableCellElement, int normalWidthPx,
                                  int maxSpannedWidthPx, float normalHeightPt) {
        final HSSFCellStyle cellStyle = cell.getCellStyle();

        String value;
        switch (cell.getCellType()) {
            case STRING:
                // XXX: enrich
                value = cell.getRichStringCellValue().getString();
                break;
            case FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        HSSFRichTextString str = cell.getRichStringCellValue();
                        if (str != null && str.length() > 0) {
                            value = (str.toString());
                        } else {
                            value = EMPTY;
                        }
                        break;
                    case NUMERIC:
                        HSSFCellStyle style = cellStyle;
                        if (style == null) {
                            value = String.valueOf(cell.getNumericCellValue());
                        } else {
                            //xcy:
                            evaluator.evaluateInCell(cell);

                            value = (_formatter.formatRawCellContents(
                                    cell.getNumericCellValue(), style.getDataFormat(),
                                    style.getDataFormatString()));
                        }
                        break;
                    case BOOLEAN:
                        value = String.valueOf(cell.getBooleanCellValue());
                        break;
                    case ERROR:
                        value = ErrorEval.getText(cell.getErrorCellValue());
                        break;
                    default:
                        logger.log(
                                POILogger.WARN,
                                "Unexpected cell cachedFormulaResultType ("
                                        + cell.getCachedFormulaResultType() + ")");
                        value = EMPTY;
                        break;
                }
                break;
            case BLANK:
                value = EMPTY;
                break;
            case NUMERIC:
                value = _formatter.formatCellValue(cell);
                break;
            case BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case ERROR:
                value = ErrorEval.getText(cell.getErrorCellValue());
                break;
            default:
                logger.log(POILogger.WARN,
                        "Unexpected cell type (" + cell.getCellType() + ")");
                return true;
        }

        final boolean noText = isEmpty(value);
        final boolean wrapInDivs = !noText && isUseDivsToSpan() && !cellStyle.getWrapText();

        final short cellStyleIndex = cellStyle.getIndex();
        if (cellStyleIndex != 0) {
            @SuppressWarnings("resource")
            HSSFWorkbook workbook = cell.getRow().getSheet().getWorkbook();
            String mainCssClass = getStyleClassName(workbook, cellStyle);

            if (wrapInDivs) {
                tableCellElement.setAttribute("class", mainCssClass + " "
                        + cssClassContainerCell);
            } else {
                tableCellElement.setAttribute("class", mainCssClass);
            }

            if (noText) {
                /*
                 * if cell style is defined (like borders, etc.) but cell text
                 * is empty, add "&nbsp;" to output, so browser won't collapse
                 * and ignore cell
                 */
                value = "\u00A0";
            }
        }

        if (isOutputLeadingSpacesAsNonBreaking() && value.startsWith(" ")) {
            StringBuilder builder = new StringBuilder();
            for (int c = 0; c < value.length(); c++) {
                if (value.charAt(c) != ' ')
                    break;
                builder.append('\u00a0');
            }

            if (value.length() != builder.length())
                builder.append(value.substring(builder.length()));

            value = builder.toString();
        }

        Text text = htmlDocumentFacade.createText(value);

        if (wrapInDivs) {
            Element outerDiv = htmlDocumentFacade.createBlock();
            outerDiv.setAttribute("class", this.cssClassContainerDiv);

            Element innerDiv = htmlDocumentFacade.createBlock();
            StringBuilder innerDivStyle = new StringBuilder();
            innerDivStyle.append("position:absolute;min-width:");
            innerDivStyle.append(normalWidthPx);
            innerDivStyle.append("px;");
            if (maxSpannedWidthPx != Integer.MAX_VALUE) {
                innerDivStyle.append("max-width:");
                innerDivStyle.append(maxSpannedWidthPx);
                innerDivStyle.append("px;");
            }
            innerDivStyle.append("overflow:hidden;max-height:");
            innerDivStyle.append(normalHeightPt);
            innerDivStyle.append("pt;white-space:nowrap;");
            ExcelToHtmlUtils.appendAlign(innerDivStyle, cellStyle.getAlignment());
            htmlDocumentFacade.addStyleClass(outerDiv, cssClassPrefixDiv, innerDivStyle.toString());

            innerDiv.appendChild(text);
            outerDiv.appendChild(innerDiv);
            tableCellElement.appendChild(outerDiv);
        } else {
            tableCellElement.appendChild(text);
        }

        return isEmpty(value) && cellStyleIndex == 0;
    }

    protected void processColumnHeaders(HSSFSheet sheet, int maxSheetColumns, Element table) {
        Element tableHeader = htmlDocumentFacade.createTableHeader();
        table.appendChild(tableHeader);

        Element tr = htmlDocumentFacade.createTableRow();

        if (isOutputRowNumbers()) {
            // empty row at left-top corner
            tr.appendChild(htmlDocumentFacade.createTableHeaderCell());
        }

        for (int c = 0; c < maxSheetColumns; c++) {
            if (!isOutputHiddenColumns() && sheet.isColumnHidden(c))
                continue;

            Element th = htmlDocumentFacade.createTableHeaderCell();
            String text = getColumnName(c);
            th.appendChild(htmlDocumentFacade.createText(text));
            tr.appendChild(th);
        }
        tableHeader.appendChild(tr);
    }

    /**
     * Creates COLGROUP element with width specified for all columns. (Except
     * first if <tt>{@link #isOutputRowNumbers()}==true</tt>)
     */
    protected void processColumnWidths(HSSFSheet sheet, int maxSheetColumns, Element table) {
        // draw COLS after we know max column number
        Element columnGroup = htmlDocumentFacade.createTableColumnGroup();
        if (isOutputRowNumbers()) {
            columnGroup.appendChild(htmlDocumentFacade.createTableColumn());
        }
        for (int c = 0; c < maxSheetColumns; c++) {
            if (!isOutputHiddenColumns() && sheet.isColumnHidden(c))
                continue;

            Element col = htmlDocumentFacade.createTableColumn();
            col.setAttribute("width", String.valueOf(getColumnWidth(sheet, c)));
            columnGroup.appendChild(col);
        }
        table.appendChild(columnGroup);
    }

    protected void processDocumentInformation(
            SummaryInformation summaryInformation) {
        if (isNotEmpty(summaryInformation.getTitle()))
            htmlDocumentFacade.setTitle(summaryInformation.getTitle());

        if (isNotEmpty(summaryInformation.getAuthor()))
            htmlDocumentFacade.addAuthor(summaryInformation.getAuthor());

        if (isNotEmpty(summaryInformation.getKeywords()))
            htmlDocumentFacade.addKeywords(summaryInformation.getKeywords());

        if (isNotEmpty(summaryInformation.getComments()))
            htmlDocumentFacade.addDescription(summaryInformation.getComments());
    }

    /**
     * @return maximum 1-base index of column that were rendered, zero if none
     */
    protected int processRow(CellRangeAddress[][] mergedRanges, HSSFRow row, Element tableRowElement) {
        final HSSFSheet sheet = row.getSheet();
        final short maxColIx = row.getLastCellNum();
        if (maxColIx <= 0)
            return 0;

        final List<Element> emptyCells = new ArrayList<Element>(maxColIx);

        if (isOutputRowNumbers()) {
            Element tableRowNumberCellElement = htmlDocumentFacade.createTableHeaderCell();
            processRowNumber(row, tableRowNumberCellElement);
            emptyCells.add(tableRowNumberCellElement);
        }

        int maxRenderedColumn = 0;
        for (int colIx = 0; colIx < maxColIx; colIx++) {
            if (!isOutputHiddenColumns() && sheet.isColumnHidden(colIx))
                continue;

            CellRangeAddress range = ExcelToHtmlUtils.getMergedRange(mergedRanges, row.getRowNum(), colIx);

            if (range != null && (range.getFirstColumn() != colIx || range.getFirstRow() != row .getRowNum()))
                continue;

            HSSFCell cell = row.getCell(colIx);

            int divWidthPx = 0;
            if (isUseDivsToSpan()) {
                divWidthPx = getColumnWidth(sheet, colIx);

                boolean hasBreaks = false;
                for (int nextColumnIndex = colIx + 1; nextColumnIndex < maxColIx; nextColumnIndex++) {
                    if (!isOutputHiddenColumns()
                            && sheet.isColumnHidden(nextColumnIndex))
                        continue;

                    if (row.getCell(nextColumnIndex) != null && !isTextEmpty(row.getCell(nextColumnIndex))) {
                        hasBreaks = true;
                        break;
                    }

                    divWidthPx += getColumnWidth(sheet, nextColumnIndex);
                }

                if (!hasBreaks)
                    divWidthPx = Integer.MAX_VALUE;
            }

            Element tableCellElement = htmlDocumentFacade.createTableCell();

            if (range != null) {
                if (range.getFirstColumn() != range.getLastColumn())
                    tableCellElement.setAttribute("colspan", String.valueOf(range.getLastColumn() - range.getFirstColumn() + 1));
                if (range.getFirstRow() != range.getLastRow())
                    tableCellElement.setAttribute("rowspan", String.valueOf(range.getLastRow() - range.getFirstRow() + 1));
            }

            boolean emptyCell;
            if (cell != null) {
                emptyCell = processCell(cell, tableCellElement, getColumnWidth(sheet, colIx), divWidthPx, row.getHeight() / 20f);
            } else {
                emptyCell = true;
            }

            if (emptyCell) {
                emptyCells.add(tableCellElement);
            } else {
                for (Element emptyCellElement : emptyCells) {
                    tableRowElement.appendChild(emptyCellElement);
                }
                emptyCells.clear();

                tableRowElement.appendChild(tableCellElement);
                maxRenderedColumn = colIx;
            }
        }

        return maxRenderedColumn + 1;
    }

    protected void processRowNumber(HSSFRow row, Element tableRowNumberCellElement) {
        tableRowNumberCellElement.setAttribute("class", "rownumber");
        Text text = htmlDocumentFacade.createText(getRowName(row));
        tableRowNumberCellElement.appendChild(text);
    }

    protected void processSheet(HSSFSheet sheet) {
        //xcy:控制是否输出头
        if (this.isOutputSheetHeaders()) {
            processSheetHeader(htmlDocumentFacade.getBody(), sheet);
        }
        final int physicalNumberOfRows = sheet.getPhysicalNumberOfRows();
        if (physicalNumberOfRows <= 0)
            return;

        Element table = htmlDocumentFacade.createTable();
        htmlDocumentFacade.addStyleClass(table, cssClassPrefixTable, "border-collapse:collapse;border-spacing:0;");

        Element tableBody = htmlDocumentFacade.createTableBody();

        final CellRangeAddress[][] mergedRanges = ExcelToHtmlUtils.buildMergedRangesMap(sheet);

        final List<Element> emptyRowElements = new ArrayList<Element>( physicalNumberOfRows);
        int maxSheetColumns = 1;
        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
            HSSFRow row = sheet.getRow(r);

            if (row == null)
                continue;

            if (!isOutputHiddenRows() && row.getZeroHeight())
                continue;

            Element tableRowElement = htmlDocumentFacade.createTableRow();
            htmlDocumentFacade.addStyleClass(tableRowElement, cssClassPrefixRow, "height:" + (row.getHeight() / 20f) + "pt;");

            int maxRowColumnNumber = processRow(mergedRanges, row, tableRowElement);

            if (maxRowColumnNumber == 0) {
                emptyRowElements.add(tableRowElement);
            } else {
                if (!emptyRowElements.isEmpty()) {
                    for (Element emptyRowElement : emptyRowElements) {
                        tableBody.appendChild(emptyRowElement);
                    }
                    emptyRowElements.clear();
                }

                tableBody.appendChild(tableRowElement);
            }
            maxSheetColumns = Math.max(maxSheetColumns, maxRowColumnNumber);
        }

        processColumnWidths(sheet, maxSheetColumns, table);

        if (isOutputColumnHeaders()) {
            processColumnHeaders(sheet, maxSheetColumns, table);
        }

        table.appendChild(tableBody);

        htmlDocumentFacade.getBody().appendChild(table);
    }

    protected void processSheetHeader(Element htmlBody, HSSFSheet sheet) {
        Element h2 = htmlDocumentFacade.createHeader2();
        h2.appendChild(htmlDocumentFacade.createText(sheet.getSheetName()));
        htmlBody.appendChild(h2);
    }

    public void processWorkbook(HSSFWorkbook workbook) {
        this.processWorkbook(workbook, workbook.getNumberOfSheets());
    }

    //xcy:
    private FormulaEvaluator evaluator;

    public void processWorkbook(HSSFWorkbook workbook, int numberOfSheets) {
        CreationHelper crateHelper = workbook.getCreationHelper();
        this.evaluator = crateHelper.createFormulaEvaluator();

        final SummaryInformation summaryInformation = workbook.getSummaryInformation();
        if (summaryInformation != null) {
            processDocumentInformation(summaryInformation);
        }

        if (isUseDivsToSpan()) {
            // prepare CSS classes for later usage
            this.cssClassContainerCell = htmlDocumentFacade.getOrCreateCssClass(cssClassPrefixCell,
                            "padding:0;margin:0;align:left;vertical-align:top;");
            this.cssClassContainerDiv = htmlDocumentFacade.getOrCreateCssClass(
                    cssClassPrefixDiv, "position:relative;");
        }

        for (int s = 0; s < numberOfSheets; s++) {
            HSSFSheet sheet = workbook.getSheetAt(s);
            processSheet(sheet);
        }

        htmlDocumentFacade.updateStylesheet();
    }

    public void setCssClassPrefixCell(String cssClassPrefixCell) {
        this.cssClassPrefixCell = cssClassPrefixCell;
    }

    public void setCssClassPrefixDiv(String cssClassPrefixDiv) {
        this.cssClassPrefixDiv = cssClassPrefixDiv;
    }

    public void setCssClassPrefixRow(String cssClassPrefixRow) {
        this.cssClassPrefixRow = cssClassPrefixRow;
    }

    public void setCssClassPrefixTable(String cssClassPrefixTable) {
        this.cssClassPrefixTable = cssClassPrefixTable;
    }

    /**
     * Allows converter to wrap content into two additional DIVs with tricky
     * styles, so it will wrap across empty cells (like in Excel).
     * <p>
     * <b>Warning:</b> after enabling this mode do not serialize result HTML
     * with INDENT=YES option, because line breaks will make additional
     * (unwanted) changes<br>
     * 翻译：
     * 允许转换器将内容包装到另外两个具有棘手样式的DIV中，因此它将换行空单元格（如Excel中）。
     * 警告：启用此模式后，请勿使用INDENT = YES选项序列化结果HTML，因为换行符将进行其他（不需要的）更改
     */
    public void setUseDivsToSpan(boolean useDivsToSpan) {
        this.useDivsToSpan = useDivsToSpan;
    }

    //xcy:add it 
    private boolean outputSheetHeaders = true;

    public boolean isOutputSheetHeaders() {
        return outputSheetHeaders;
    }

    public void setOutputSheetHeaders(boolean outputSheetHeaders) {
        this.outputSheetHeaders = outputSheetHeaders;
    }

    static final String EMPTY = "";

    private boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    private boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
