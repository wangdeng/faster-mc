package com.ai.iisc.modules.andcolleges.utils;

import com.ai.iisc.core.exception.BaseException;
import com.ai.iisc.core.utils.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.net.URLEncoder;
import java.util.*;
/**
 * 类描述
 * @author: author
 * @since: 2018/9/8 11:16
 **/
public abstract class ExcelParser<T> {
    private String sheetName;
    private final String[] COLUMN_IMPORT_NAMES;
    private final String[] COLUMN_EXPORT_NAMES;
    private final Map<String,Integer>INDEX_MAP;
    private final boolean[] NOT_NULLS;
    private final boolean[] NUM_TYPES;
    private final int COL_SIZE;
    private Class<T> classType;
    public Integer getInt(String str){
        return str==null||str.isEmpty()?0:Integer.valueOf(str);
    }
    public String getString(Object value){
        return value==null?"":String.valueOf(value);
    }
    public ExcelParser(String sheetName, String... notNulls) {
        this.sheetName = sheetName;
        classType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        LinkedHashMap<String,String> map = new LinkedHashMap<String,String>();
        INDEX_MAP=new HashMap<>();
        setAttribute(map,getObject());
        Set<String> keySet=map.keySet();
        COL_SIZE=keySet.size();
        COLUMN_IMPORT_NAMES=new String[COL_SIZE];
        COLUMN_EXPORT_NAMES=new String[COL_SIZE];
        NOT_NULLS=new boolean[COL_SIZE];
        NUM_TYPES=new boolean[COL_SIZE];
        int i=0;
        boolean hasNotNulls=notNulls.length>0;
        Set<String> notNullSet=new HashSet<>();
        for(String notNullKey:notNulls){
            notNullSet.add(notNullKey);
        }
        for(String key:keySet){
            INDEX_MAP.put(key,i);
            COLUMN_EXPORT_NAMES[i]=key;
            
//            if(key.contains("手机号")||key.contains("电话")){
//                NUM_TYPES[i]=true;
//            }
//            if(hasNotNulls&&notNullSet.contains(key)){
//                NOT_NULLS[i]=true;
//                key=new StringBuilder(key).append("（必填）").toString();
//                INDEX_MAP.put(key,i);
//            }
            COLUMN_IMPORT_NAMES[i]= key;
            i++;
        }
    }

    private T getObject() {
        T returnObject = null;
        try {
            returnObject = classType.newInstance();
        } catch (Exception e) {
        }
        return returnObject;
    }

    public abstract void getAttribute(Map<String,String> map, T t);

    public abstract void setAttribute(Map<String,String> map, T t);

    public List<T> importExcel(InputStream inputStream) {
        List<T> list=null;
        try{
            list= importExcel(new XSSFWorkbook(inputStream));
        } catch (IOException e) {
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
        return list;
    }
    public List<T> importExcel(MultipartFile file) {
        List<T> list=null;
        try{
            list= importExcel(new XSSFWorkbook(ExcelUtil.multipartToFile(file)));
        } catch (IOException e) {
        } catch (InvalidFormatException e) {
        }
        return list;
    }
    private List<T> importExcel(XSSFWorkbook wb){
        XSSFSheet sheet=wb.getSheetAt(0);
        int rows=sheet.getLastRowNum()+1;
        Map<Integer,String> columnInexMap=new HashMap<>();
        int columnSize=0;
        List<T> resultList=new ArrayList<>();
        for(int i=0;i<rows;i++){
            Row row=sheet.getRow(i);
            if(i==0){
                columnSize =row.getLastCellNum();
                if(columnSize!=COL_SIZE){
                    return null;
                }
                for(int j=0;j<columnSize;j++){
                    Cell cell=row.getCell(j);
                    if(cell==null){
                        throw new BaseException("message.groupsend.import.columnTitleNotMatch");
                    }
                    String cellValue=cell.getStringCellValue();
                    if(cellValue==null){
                        throw new BaseException("message.groupsend.import.columnTitleNotMatch");
                    }
                    cellValue=cellValue.trim();
                    if(!INDEX_MAP.containsKey(cellValue)){
                        throw new BaseException("message.groupsend.import.columnTitleNotFound",cellValue);
                    }
                    columnInexMap.put(j,cellValue);
                }
            }else{
                Map<String,String> dataMap=new HashMap<>();
                for(int j=0;j<columnSize;j++){
                    Cell cell=row.getCell(j);
                    String value=null;

                    if(cell!=null){
                        cell.setCellType(CellType.STRING);
                        value=cell.getStringCellValue();
                    }
                    String columnName=columnInexMap.get(j);
                    int index=INDEX_MAP.get(columnName);
                    if(NOT_NULLS[index]&&value==null){
                        throw new BaseException("message.groupsend.import.columnValueNotNull",cell.getRowIndex(),cell.getColumnIndex(),columnName);
                    }
                    dataMap.put(columnName,value);
                }
                T t=getObject();
                getAttribute(dataMap,t);
                resultList.add(t);
            }
        }
        return  resultList;
    }

    public void downloadExportExcel(OutputStream outputStream, List<T> resultList) {
        downloadExportExcel(outputStream,resultList,true);
    }
    public void downloadExportExcel(HttpServletResponse response,List<T> resultList){
        String workbookName = StringUtils.chain(sheetName, ".xlsx");
        try {
            String httpFileName=URLEncoder.encode(workbookName, "UTF-8").replace("+", "%20");;
            response.setContentType("application/x-msdownload");
            response.setHeader("Pragma", URLEncoder.encode(workbookName, "UTF-8"));
            response.setHeader("Content-disposition","attachment; filename="+httpFileName);
            downloadExportExcel(response.getOutputStream(),resultList,false);
            response.flushBuffer();
        } catch (IOException e) {
        }
    }

    private void downloadExportExcel(OutputStream outputStream,List<T> resultList,boolean closeStream){
        XSSFWorkbook wb = new XSSFWorkbook(); 
        XSSFSheet sheet = wb.createSheet(StringUtils.chain(sheetName,"-导出详情"));
        
        XSSFRow firstRow = sheet.createRow(0);
        firstRow.setHeightInPoints((short) 16);
        XSSFFont firstRowFont = ExcelUtil.getHeaderFontOne(wb);
        XSSFCellStyle firstRowStyle = ExcelUtil.getFirstRowStyle(wb, firstRowFont, IndexedColors.YELLOW);
        DataFormat fmt = wb.createDataFormat();
        CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setDataFormat(fmt.getFormat("@"));
        CellStyle numStyle = wb.createCellStyle();
        numStyle.setDataFormat(fmt.getFormat("0"));
        double[] countStringLengthArr = new double[COL_SIZE];
        
        for(int i=0;i<COL_SIZE;i++){
            XSSFCell cell=firstRow.createCell(i);
            cell.setCellStyle(firstRowStyle);
            String cellValue=COLUMN_EXPORT_NAMES[i];
            cell.setCellValue(cellValue);
            
            if(NUM_TYPES[i]){
                sheet.getColumnHelper().setColDefaultStyle(i, numStyle);
            }else{
                sheet.getColumnHelper().setColDefaultStyle(i, cellStyle);
            }
            ExcelUtil.checkColumnWith(countStringLengthArr, cellValue, i, true);
        }
        if(resultList!=null&&!resultList.isEmpty()){
            for(int j=0,len=resultList.size();j<len;j++){
                XSSFRow contentRow = sheet.createRow(j + 1);
                Map<String,String> contentMap=new HashMap<>();
                T resultObject=resultList.get(j);
                setAttribute(contentMap,resultObject);
                for(Map.Entry<String,String> entry:contentMap.entrySet()){
                    String key=entry.getKey();
                    String value=getString(entry.getValue());
                    if(!INDEX_MAP.containsKey(key)){
                        continue;
                    }
                    int index=INDEX_MAP.get(entry.getKey());
                    XSSFCell cell = contentRow.createCell(index);
                    cell.setCellValue(value);
                    ExcelUtil.checkColumnWith(countStringLengthArr, value, index, false);
                }
            }
            
            ExcelUtil.setBorder(sheet, 0, resultList.size(), 0, COL_SIZE - 1);

            for (int j = 0; j < COL_SIZE; j++) {
                
                sheet.setColumnWidth(j, (int)countStringLengthArr[j] * 256);
            }
            sheet.createFreezePane(0, 1);
        }
        writeExcelAndClose(wb,outputStream,closeStream);

    }
    private void writeExcelAndClose(XSSFWorkbook wb,OutputStream outputStream,boolean closeStream){
        try {
            if(outputStream!=null){
                wb.write(outputStream);
                if(closeStream){
                    outputStream.flush();
                    outputStream.close();
                }
            }
        } catch (IOException e) {
        }
    }
    public void downloadImportExcel(OutputStream outputStream) {
        downloadImportExcel(outputStream,true);
    }
    public void downloadImportExcel(HttpServletResponse response){
        String workbookName = StringUtils.chain(sheetName, ".xlsx");
        try {
            String httpFileName=URLEncoder.encode(workbookName, "UTF-8").replace("+", "%20");;
            response.setContentType("application/x-msdownload");
            response.setHeader("Pragma", URLEncoder.encode(workbookName, "UTF-8"));
            response.setHeader("Content-disposition","attachment; filename="+httpFileName);
            downloadImportExcel(response.getOutputStream(),false);
            response.flushBuffer();
        } catch (IOException e) {
        }
    }
    private void downloadImportExcel(OutputStream outputStream,boolean closeStream) {
        XSSFWorkbook wb = new XSSFWorkbook(); 
        XSSFSheet sheet = wb.createSheet(StringUtils.chain(sheetName,"-导入模板"));
        
        XSSFRow firstRow = sheet.createRow(0);
        firstRow.setHeightInPoints((short) 16);
        XSSFFont firstRowFont = ExcelUtil.getHeaderFontOne(wb);
        XSSFCellStyle firstRowStyle = ExcelUtil.getFirstRowStyle(wb, firstRowFont, IndexedColors.YELLOW);
        DataFormat fmt = wb.createDataFormat();
        CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setDataFormat(fmt.getFormat("@"));
        CellStyle numStyle = wb.createCellStyle();
        numStyle.setDataFormat(fmt.getFormat("0"));
        
        XSSFFont redFont=wb.createFont();
        redFont.setColor(IndexedColors.RED.index);
        
        for(int i=0;i<COL_SIZE;i++){
            XSSFCell cell=firstRow.createCell(i);
            cell.setCellStyle(firstRowStyle);
            String cellValue=COLUMN_IMPORT_NAMES[i];
            if(NOT_NULLS[i]){
                XSSFRichTextString multiColorString=new XSSFRichTextString(cellValue);
                multiColorString.applyFont(cellValue.length()-4, cellValue.length(), redFont);
                cell.setCellValue(multiColorString);
            }else{
                cell.setCellValue(cellValue);
            }
            
            if(NUM_TYPES[i]){
                sheet.getColumnHelper().setColDefaultStyle(i, numStyle);
            }else{
                sheet.getColumnHelper().setColDefaultStyle(i, cellStyle);
            }
            sheet.setColumnWidth(i, (int)(StringUtils.getLengthGBK(cellValue)*1.3*256));
        }
        writeExcelAndClose(wb,outputStream,closeStream);
    }
















//








}
