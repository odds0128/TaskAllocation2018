import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.xmlbeans.impl.xb.xsdschema.All;

import java.io.*;
import java.util.*;

/**
 * OutPutクラス
 * Singletonで実装
 * 結果の画面出力とファイル出力を管理
 */
public class OutPut implements SetParam {
    static Workbook book = null;
    static FileOutputStream fout = null;

    static OutPut _singleton = new OutPut();

    static int index = 0;

    static int[] finishedTasksArray = new int[WRITING_TIMES];
    static int[] disposedTasksArray = new int[WRITING_TIMES];
    static int[] overflownTasksArray = new int[WRITING_TIMES];
    static int[] messagesArray = new int[WRITING_TIMES];
    static double[] communicationDelayArray = new double[WRITING_TIMES];
    static int[] leaderNumArray = new int[WRITING_TIMES];
    static int[] memberNumArray = new int[WRITING_TIMES];
    static int[] leaderNumInDepopulatedAreaArray = new int[WRITING_TIMES];
    static int[] memberNumInDepopulatedAreaArray = new int[WRITING_TIMES];
    static int[] leaderNumInPopulatedAreaArray = new int[WRITING_TIMES];
    static int[] memberNumInPopulatedAreaArray = new int[WRITING_TIMES];
    static int[] neetMembersArray = new int[WRITING_TIMES];
    static int[] reciprocalistsArray = new int[WRITING_TIMES];
    static int[] rationalistsArray = new int[WRITING_TIMES];
    static int[] reciprocalMembersArray = new int[WRITING_TIMES];
    static int[] finishedTasksInDepopulatedAreaArray = new int[WRITING_TIMES];
    static int[] finishedTasksInPopulatedAreaArray = new int[WRITING_TIMES];
    static int[] tempTaskExecutionTimeArray = new int[WRITING_TIMES];
    static int[] taskExecutionTimeArray = new int[WRITING_TIMES];
    static int   taskExecutionTimes = 0;

    static void aggregateAgentData(List<Agent> agents) {
        leaderNumArray[index] += Agent._leader_num;
        neetMembersArray[index] += Agent.countNEETmembers(agents, MAX_TURN_NUM / WRITING_TIMES);
/*        memberNumArray[index] += Agent._member_num;

        int a = Agent.countLeadersInDepopulatedArea(agents);
        int b = Agent.countLeadersInPopulatedArea(agents);

        leaderNumInDepopulatedAreaArray[index] += a;
        memberNumInDepopulatedAreaArray[index] += (Agent._lonelyAgents.size() - a);
        leaderNumInPopulatedAreaArray[index]   += b;
        memberNumInPopulatedAreaArray[index]   += (Agent._lonelyAgents.size() - b);
        // */
    }

    static void aggregateData(int ft, int dt, int ot, int rm, int ftida, int ftipa) {
        finishedTasksArray[index] += ft;
        communicationDelayArray[index] += TransmissionPath.getCT();
        disposedTasksArray[index] += dt;
        overflownTasksArray[index] += ot;
        /* messagesArray[index] += TransmissionPath.messageNum;
        reciprocalistsArray[index] += Agent._recipro_num;
        rationalistsArray[index] += Agent._rational_num;
        reciprocalMembersArray[index] += rm;
        finishedTasksInDepopulatedAreaArray[index] += ftida ;
        finishedTasksInPopulatedAreaArray[index]   += ftipa ;
// */
    }

    static void aggregateTaskExecutionTime(Agent leader){
        if( leader.mySubTask != null ){
            tempTaskExecutionTimeArray[index] += leader.executionTime;
            taskExecutionTimes++;
        }
        for ( Map.Entry<Agent, SubTask> al : leader.preAllocations.entrySet()) {
            tempTaskExecutionTimeArray[index] += leader.calcExecutionTime(al.getKey(), al.getValue());
            taskExecutionTimes++;
        }
//        System.out.println(tempTaskExecutionTimeArray[index] + ", " + taskExecutionTimes);
    }

    static void indexIncrement() {
        if( taskExecutionTimes != 0 ){
            taskExecutionTimeArray[index] += tempTaskExecutionTimeArray[index]/taskExecutionTimes;
//            System.out.println(taskExecutionTimeArray[index]);
        }
        taskExecutionTimes = 0;
        index = (index + 1) % WRITING_TIMES;
    }

    /**
     * checkTaskメソッド
     * 現在タスクキューにあるタスクのサブタスク数とその必要リソースの表示.
     *
     * @param taskQueue
     */
    static void checkTask(Queue<Task> taskQueue) {
        int num = taskQueue.size();
        System.out.println("Queuesize: " + num);
        for (int i = 0; i < num; i++) {
            Task temp = taskQueue.poll();
            System.out.println(temp);
            taskQueue.add(temp);
        }
        System.out.println("  Remains: " + taskQueue.size());
    }

    /**
     * checkAgentメソッド
     * 二次元配列の場合
     */
    static void checkGrids(Agent[][] grids) {
        System.out.println("Total Agents is " + Agent._id);
        System.out.println("Leaders is " + Agent._leader_num + ", Members is " + Agent._member_num);
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COLUMN; j++) {
                if (grids[i][j] == null) System.out.print(" 　 ");
                else System.out.print(String.format("%3d ", grids[i][j].id));
            }
            System.out.println();
        }
    }

    static void checkDelay(int[][] delays) {
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.print("ID: " + i + "...");
            for (int j = 0; j < AGENT_NUM; j++) {
                System.out.print(delays[i][j] + ", ");
            }
            System.out.println();
        }
    }

    /**
     * checkAgentメソッド
     *
     * @param agents
     */
    static void checkAgent(List<Agent> agents) {
        List<Agent> temp = new ArrayList<>(agents);
        System.out.println("Total Agents is " + Agent._id);
        System.out.println("Leaders is " + Agent._leader_num + ", Members is " + Agent._member_num + ", Resources : ");


        for (Agent agent : agents) {
            for (int i = 0; i < RESOURCE_TYPES; i++) System.out.print(agent.res[i] + ", ");
            System.out.println();
        }
// */

/*
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.print("ID : " + i + ", ");
            for( int j = 0; j < AGENT_NUM; j++ ){
                System.out.print(String.format("%3d", Manager.delays[i][j]));
            }
            System.out.println();
        }
// */

        Collections.sort(temp, new AgentComparator());

/*
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.print(temp.get(i).id +", s: " + ProposedMethod2.dlMean[i] + " ... ");
            for( LearnedDistance ld: ProposedMethod2.dLearned[i] ){
                if( ld.getDistance() > 2 ) break;
                System.out.print(ld);
            }
            System.out.println();
        }
// */
/*        for (Agent agent : agents) {
            System.out.print("ID " + agent.id + ", e_l " + String.format("%.2f", agent.e_leader));
            System.out.print(", e_m " + String.format("%.2f", agent.e_member));
            System.out.println();
        }
// */
    }

    static void checkTeam(Agent leader) {
        System.out.print(leader.id + " and ");
        for (Agent mem : leader.teamMembers) {
            System.out.print(mem.id + ", ");
        }
        System.out.println("are good team!");

        if (leader.mySubTask != null) {
            System.out.println(" leader: " + leader + "→" + leader.mySubTask + ": " + leader.calcExecutionTime(leader, leader.mySubTask) + "[tick(s)]");
        } else {
            System.out.println(" leader: " + leader);
        }
        for ( Map.Entry<Agent, SubTask> al : leader.preAllocations.entrySet()) {
            System.out.println(" member: " + al.getKey() + "→" + al.getValue() + ": " + leader.calcExecutionTime(al.getKey(), al.getValue()) + "[tick(s)]");
        }
        // */
    }

    static void writeResults(Strategy st) {
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        String fileName = st.getClass().getName();
        try {
            String currentPath = System.getProperty("user.dir");
            fw = new FileWriter(currentPath + "/out/results/" + fileName + ", λ=" + String.format("%.2f", (double)ADDITIONAL_TASK_NUM/TASK_ADDITION_SPAN) + ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            pw.println("turn" + ", "
                            + "FinishedTasks" + ", " + "DisposedTasks" + ", " + "OverflownTasks" + ", "
                            + "CommunicationTime" + ", " + "ExecutionTime" + ","
                            + "Leader" + ", " // + "Member"                            + ", "
                            + "NEET Members" + ", "
                    // + "Lonely leaders"                    + ", " + "Lonely members"                    + ", "
                    // + "Accompanied leaders"               + ", " + "Accompanied members"               + ", "
                    // + "Reciprocal"                        + ", " + "Rational"                          + ", " + "ReciprocalMembers" + ","
                    // + "FinishedTasks in depopulated area" + ", " + "FinishedTasks in populated area"   + ", "
            );
            for (int i = 0; i < WRITING_TIMES; i++) {
                pw.println((i + 1) * (MAX_TURN_NUM / WRITING_TIMES) + ", "
                                + finishedTasksArray[i] / EXECUTION_TIMES + ", "
                                + disposedTasksArray[i] / EXECUTION_TIMES + ", "
                                + overflownTasksArray[i] / EXECUTION_TIMES + ", "
                                + communicationDelayArray[i] / (double) EXECUTION_TIMES + ", "
                                + (double) taskExecutionTimeArray[i]  / EXECUTION_TIMES + ", "
                                + leaderNumArray[i] / EXECUTION_TIMES + ", "
                                + neetMembersArray[i] / EXECUTION_TIMES + ", "
                        /*                    + memberNumArray[i]                      / EXECUTION_TIMES + ", "
                    + leaderNumInDepopulatedAreaArray[i]     / EXECUTION_TIMES + ", "
                    + memberNumInDepopulatedAreaArray[i]     / EXECUTION_TIMES + ", "
                    + leaderNumInPopulatedAreaArray[i]       / EXECUTION_TIMES + ", "
                    + memberNumInPopulatedAreaArray[i]       / EXECUTION_TIMES + ", "
                    + reciprocalistsArray[i]                 / EXECUTION_TIMES + ", "
                    + rationalistsArray[i]                   / EXECUTION_TIMES + ", "
                    + reciprocalMembersArray[i]              / EXECUTION_TIMES + ", "
                    + finishedTasksInDepopulatedAreaArray[i] / EXECUTION_TIMES + ", "
                    + finishedTasksInPopulatedAreaArray[i]   / EXECUTION_TIMES + ", "
// */
                );
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    static void writeResultsX(Strategy st) throws FileNotFoundException, IOException {
        String outputFilePath = _singleton.setPath("results", st.getClass().getName() );

        try {
            book = new SXSSFWorkbook();
            Font font = book.createFont();
            font.setFontName("ＭＳ ゴシック");
            font.setFontHeightInPoints((short) 9);

            DataFormat format = book.createDataFormat();

            //ヘッダ文字列用のスタイル
            CellStyle style_header = book.createCellStyle();
            style_header.setBorderBottom(BorderStyle.THIN);
            OutPut.setBorder(style_header, BorderStyle.THIN);
            style_header.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_CORNFLOWER_BLUE.getIndex());
            style_header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style_header.setVerticalAlignment(VerticalAlignment.TOP);
            style_header.setFont(font);

            //文字列用のスタイル
            CellStyle style_string = book.createCellStyle();
            OutPut.setBorder(style_string, BorderStyle.THIN);
            style_string.setVerticalAlignment(VerticalAlignment.TOP);
            style_string.setFont(font);

            //改行が入った文字列用のスタイル
            CellStyle style_string_wrap = book.createCellStyle();
            OutPut.setBorder(style_string_wrap, BorderStyle.THIN);
            style_string_wrap.setVerticalAlignment(VerticalAlignment.TOP);
            style_string_wrap.setWrapText(true);
            style_string_wrap.setFont(font);

            //整数用のスタイル
            CellStyle style_int = book.createCellStyle();
            OutPut.setBorder(style_int, BorderStyle.THIN);
            style_int.setDataFormat(format.getFormat("###0;-###0"));
            style_int.setVerticalAlignment(VerticalAlignment.TOP);
            style_int.setFont(font);

            //小数用のスタイル
            CellStyle style_double = book.createCellStyle();
            OutPut.setBorder(style_double, BorderStyle.THIN);
            style_double.setDataFormat(format.getFormat("###0.0;-###0.0"));
            style_double.setVerticalAlignment(VerticalAlignment.TOP);
            style_double.setFont(font);

            //円表示用のスタイル
            CellStyle style_yen = book.createCellStyle();
            OutPut.setBorder(style_yen, BorderStyle.THIN);
            style_yen.setDataFormat(format.getFormat("\"\\\"###0;\"\\\"-###0"));
            style_yen.setVerticalAlignment(VerticalAlignment.TOP);
            style_yen.setFont(font);

            //パーセント表示用のスタイル
            CellStyle style_percent = book.createCellStyle();
            OutPut.setBorder(style_percent, BorderStyle.THIN);
            style_percent.setDataFormat(format.getFormat("0.0%"));
            style_percent.setVerticalAlignment(VerticalAlignment.TOP);
            style_percent.setFont(font);

            //日時表示用のスタイル
            CellStyle style_datetime = book.createCellStyle();
            OutPut.setBorder(style_datetime, BorderStyle.THIN);
            style_datetime.setDataFormat(format.getFormat("yyyy/mm/dd hh:mm:ss"));
            style_datetime.setVerticalAlignment(VerticalAlignment.TOP);
            style_datetime.setFont(font);

            Row row;
            int rowNumber = 0;
            int colNumber = 0;

            Sheet sheet;

            sheet = book.createSheet();
            if (sheet instanceof SXSSFSheet) {
                ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
            }
            //シート名称の設定
            book.setSheetName(0, st.getClass().getName());

            row = sheet.createRow(rowNumber);
            _singleton.writeCell(row, colNumber++, style_header, "Turn");
            _singleton.writeCell(row, colNumber++, style_header, "Finished tasks");
            _singleton.writeCell(row, colNumber++, style_header, "Disposed tasks");
            _singleton.writeCell(row, colNumber++, style_header, "Overflown tasks");
            _singleton.writeCell(row, colNumber++, style_header, "Communication time");
            _singleton.writeCell(row, colNumber++, style_header, "Execution time");
            _singleton.writeCell(row, colNumber++, style_header, "Leaders");
            _singleton.writeCell(row, colNumber++, style_header, "NEET member");

            //ウィンドウ枠の固定
            sheet.createFreezePane(1, 1);

            //ヘッダ行にオートフィルタの設定
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, colNumber));

            //列幅の自動調整
            for (int j = 0; j <= colNumber; j++) {
                sheet.autoSizeColumn(j, true);
            }

            // 結果を書き込んでいく
            for (int wt = 0; wt < WRITING_TIMES; wt++) {
                rowNumber++;
                colNumber = 0;
                row = sheet.createRow(rowNumber);

                _singleton.writeCell(row, colNumber++, style_int, (wt + 1) * (MAX_TURN_NUM    / WRITING_TIMES));
                _singleton.writeCell(row, colNumber++, style_int, finishedTasksArray[wt]      / EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_int, disposedTasksArray[wt]      / EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_int, overflownTasksArray[wt]     / EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_double, communicationDelayArray[wt] / (double) EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_double, (double) taskExecutionTimeArray[wt]  / EXECUTION_TIMES );
                _singleton.writeCell(row, colNumber++, style_int, leaderNumArray[wt]          / EXECUTION_TIMES);
                _singleton.writeCell(row, colNumber++, style_int, neetMembersArray[wt]        / EXECUTION_TIMES);

                //列幅の自動調整
                for (int k = 0; k <= colNumber; k++) {
                    sheet.autoSizeColumn(k, true);
                }
            }
            //ファイル出力
            fout = new FileOutputStream(outputFilePath);
            book.write(fout);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                }
            }
            if (book != null) {
                try {
                    /*
                        SXSSFWorkbookはメモリ空間を節約する代わりにテンポラリファイルを大量に生成するため、
                        不要になった段階でdisposeしてテンポラリファイルを削除する必要がある
                     */
                    ((SXSSFWorkbook) book).dispose();
                } catch (Exception e) {
                }
            }
        }
    }

    static void writeGraphInformation(List<Agent> agents, String fp) throws FileNotFoundException, IOException {
        String currentPath = System.getProperty("user.dir");
        String outputFilePath = currentPath + "/out/relationships/" + fp + ".xlsx";
        Edge edge = new Edge();
        edge.makeEdge(agents);
        try {
            book = new SXSSFWorkbook();
            Font font = book.createFont();
            font.setFontName("ＭＳ ゴシック");
            font.setFontHeightInPoints((short) 9);

            DataFormat format = book.createDataFormat();

            //ヘッダ文字列用のスタイル
            CellStyle style_header = book.createCellStyle();
            style_header.setBorderBottom(BorderStyle.THIN);
            OutPut.setBorder(style_header, BorderStyle.THIN);
            style_header.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_CORNFLOWER_BLUE.getIndex());
            style_header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style_header.setVerticalAlignment(VerticalAlignment.TOP);
            style_header.setFont(font);

            //文字列用のスタイル
            CellStyle style_string = book.createCellStyle();
            OutPut.setBorder(style_string, BorderStyle.THIN);
            style_string.setVerticalAlignment(VerticalAlignment.TOP);
            style_string.setFont(font);

            //改行が入った文字列用のスタイル
            CellStyle style_string_wrap = book.createCellStyle();
            OutPut.setBorder(style_string_wrap, BorderStyle.THIN);
            style_string_wrap.setVerticalAlignment(VerticalAlignment.TOP);
            style_string_wrap.setWrapText(true);
            style_string_wrap.setFont(font);

            //整数用のスタイル
            CellStyle style_int = book.createCellStyle();
            OutPut.setBorder(style_int, BorderStyle.THIN);
            style_int.setDataFormat(format.getFormat("###0;-###0"));
            style_int.setVerticalAlignment(VerticalAlignment.TOP);
            style_int.setFont(font);

            //小数用のスタイル
            CellStyle style_double = book.createCellStyle();
            OutPut.setBorder(style_double, BorderStyle.THIN);
            style_double.setDataFormat(format.getFormat("###0.0;-###0.0"));
            style_double.setVerticalAlignment(VerticalAlignment.TOP);
            style_double.setFont(font);

            //円表示用のスタイル
            CellStyle style_yen = book.createCellStyle();
            OutPut.setBorder(style_yen, BorderStyle.THIN);
            style_yen.setDataFormat(format.getFormat("\"\\\"###0;\"\\\"-###0"));
            style_yen.setVerticalAlignment(VerticalAlignment.TOP);
            style_yen.setFont(font);

            //パーセント表示用のスタイル
            CellStyle style_percent = book.createCellStyle();
            OutPut.setBorder(style_percent, BorderStyle.THIN);
            style_percent.setDataFormat(format.getFormat("0.0%"));
            style_percent.setVerticalAlignment(VerticalAlignment.TOP);
            style_percent.setFont(font);

            //日時表示用のスタイル
            CellStyle style_datetime = book.createCellStyle();
            OutPut.setBorder(style_datetime, BorderStyle.THIN);
            style_datetime.setDataFormat(format.getFormat("yyyy/mm/dd hh:mm:ss"));
            style_datetime.setVerticalAlignment(VerticalAlignment.TOP);
            style_datetime.setFont(font);

            Row row;
            int rowNumber;
            Cell cell;
            int colNumber;


            Sheet sheet;

            for (int i = 0; i < 3; i++) {
                sheet = book.createSheet();
                if (sheet instanceof SXSSFSheet) {
                    ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
                }
                //シート名称の設定
                if (i == 0) book.setSheetName(i, "nodes");
                else if (i == 1) book.setSheetName(i, "edges");
                else if (i == 2) book.setSheetName(i, "reciprocalEdges");

                //ヘッダ行の作成
                rowNumber = 0;
                colNumber = 0;

                row = sheet.createRow(rowNumber);
                cell = row.createCell(colNumber++);
                cell.setCellStyle(style_header);
                cell.setCellType(CellType.STRING);
                if (i == 0) cell.setCellValue("Node id");
                else cell.setCellValue("Edge id");

                cell = row.createCell(colNumber++);
                cell.setCellStyle(style_header);
                cell.setCellType(CellType.STRING);
                if (i == 0) cell.setCellValue("Node color");
                else cell.setCellValue("Source Node id");

                cell = row.createCell(colNumber++);
                cell.setCellStyle(style_header);
                cell.setCellType(CellType.STRING);
                if (i == 0) cell.setCellValue("Node shape");
                else cell.setCellValue("Target Node id");

                if (i == 0) {
                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" x-coordinate ");

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" y-coordinate ");
/*
                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" leader id");
// */
                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" delay to leader ");
/*
                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" is lonely or not");

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" is accompanied or not");
// */
                    for( int j = 0; j < RESOURCE_TYPES; j++ ) {
                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_header);
                        cell.setCellType(CellType.STRING);
                        cell.setCellValue(" Resources " + j );
                    }
                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.NUMERIC);
                    cell.setCellValue(" Times ");

                } else {
                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" length ");

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" times ");

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue("Target Node id again");
                }

                //ウィンドウ枠の固定
                sheet.createFreezePane(1, 1);

                //ヘッダ行にオートフィルタの設定
                sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, colNumber));

                //列幅の自動調整
                for (int j = 0; j <= colNumber; j++) {
                    sheet.autoSizeColumn(j, true);
                }

                //nodeシートへの書き込み
                if (i == 0) {
                    for (Agent agent : agents) {
                        rowNumber++;
                        colNumber = 0;
                        row = sheet.createRow(rowNumber);
                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_int);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.id);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.STRING);
                        if (agent.e_leader > agent.e_member) cell.setCellValue("Red");
                        else if (agent.principle == RATIONAL) cell.setCellValue("Green");
                        else if (agent.principle == RECIPROCAL) cell.setCellValue("Blue");

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.STRING);
                        if (agent.e_leader > agent.e_member) cell.setCellValue("Circle");
                        else if (agent.principle == RATIONAL) cell.setCellValue("Square");
                        else if (agent.principle == RECIPROCAL) cell.setCellValue("Triangle");

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.x * 10);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.y * 10);

                        /*
                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        if (agent.e_leader > agent.e_member) cell.setCellValue(agent.id * 3);
                        else if (agent.leader != null) cell.setCellValue(agent.leader.id * 3);
// */
                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        if (agent.e_leader > agent.e_member) cell.setCellValue(0);
                        else if (agent.leader != null) cell.setCellValue(Manager.delays[agent.id][agent.leader.id] );
/*
                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.isLonely);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.isAccompanied);
// */
                        for( int j = 0; j < RESOURCE_TYPES; j++ ) {
                            cell = row.createCell(colNumber++);
                            cell.setCellStyle(style_string);
                            cell.setCellType(CellType.NUMERIC);
                            cell.setCellValue(agent.res[j]);
                        }
                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.didTasksAsMember);

                        //列幅の自動調整
                        for (int k = 0; k <= colNumber; k++) {
                            sheet.autoSizeColumn(k, true);
                        }
                    }
                }
                // edgeシートへの書き込み
                else if (i == 1) {
                    for (int j = 0; j < edge.from_id.size(); j++) {
                        rowNumber++;
                        colNumber = 0;
                        row = sheet.createRow(rowNumber);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_int);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(j);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(edge.from_id.get(j));

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(edge.to_id.get(j));

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(edge.delays.get(j));

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(edge.times.get(j));

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(edge.to_id.get(j));


                        //列幅の自動調整
                        for (int k = 0; k <= colNumber; k++) {
                            sheet.autoSizeColumn(k, true);
                        }
                    }
                } else if (i == 2) {
                    for (int j = 0; j < edge.from_id.size(); j++) {
                        if (edge.isRecipro.get(j) != true) continue;
                        rowNumber++;
                        colNumber = 0;
                        row = sheet.createRow(rowNumber);
                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_int);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(j);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(edge.from_id.get(j));

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(edge.to_id.get(j));

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(edge.delays.get(j));

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(edge.times.get(j));

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string_wrap);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(edge.to_id.get(j));

                        //列幅の自動調整
                        for (int k = 0; k <= colNumber; k++) {
                            sheet.autoSizeColumn(k, true);
                        }
                    }
                }
            }
            //ファイル出力
            fout = new FileOutputStream(outputFilePath);
            book.write(fout);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                }
            }
            if (book != null) {
                try {
                    /*
                        SXSSFWorkbookはメモリ空間を節約する代わりにテンポラリファイルを大量に生成するため、
                        不要になった段階でdisposeしてテンポラリファイルを削除する必要がある
                     */
                    ((SXSSFWorkbook) book).dispose();
                } catch (Exception e) {
                }
            }
        }
        edge = null;
    }

    static void writeDelays(int[][] delays) {
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        try {
            String currentPath = System.getProperty("user.dir");
            fw = new FileWriter(currentPath + "/out/results/communicationDelay=" + MAX_DELAY + ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

/*            pw.println("delay" + ", " + " count ");
            for (int i = 1; i < MAX_DELAY + 1; i++) {
                pw.println(i + ", " + dCounts[i] / 2);
            }
            pw.println();
// */
            pw.print("id");
            for (int i = 0; i < AGENT_NUM; i++) pw.print(", " + i);
            pw.println();
            for (int i = 0; i < AGENT_NUM; i++) {
                pw.print(i + ", ");
                for (int j = 0; j < AGENT_NUM; j++) {
                    pw.print(delays[i][j] + ", ");
                }
                pw.println();
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    static void writeDelaysAndRels(int[][] delays, List<Agent> agents, Strategy st) {
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        String fileName = st.getClass().getName();

        try {
            String currentPath = System.getProperty("user.dir");
            fw = new FileWriter(currentPath + "/out/results/d&r " + fileName + ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            for (int from = 0; from < AGENT_NUM; from++) {
                for (int to = 0; to < AGENT_NUM; to++) {
                    pw.println(delays[from][to] + ", " + agents.get(from).reliabilities[to]);
                }
            }

/*            List[] delayLists = new ArrayList[MAX_DELAY + 1];
            for( int i = 0; i < delayLists.length; i++ ){
                delayLists[i] = new ArrayList<>();
            }

            for(int from = 0; from < AGENT_NUM; from++){
                for( int to = 0; to < AGENT_NUM; to++ ){
                    delayLists[delays[from][to]].add(agents.get(from).reliabilities[to]);
                }
            }
            System.out.println(delayLists.length);
            for( int i = 1; i < delayLists.length ; i++ ){
                pw.print( i  );
                for( int j = 0; j < delayLists[i].size(); j++ ){
                    pw.print( ", " + delayLists[i].get(j) );
                }
                pw.println();
            }
            // */
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    static void writeReliabilities(List<Agent> agents, Strategy st) {
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        String fileName = st.getClass().getName();
        try {
            String currentPath = System.getProperty("user.dir");
            fw = new FileWriter(currentPath + "/out/results/rel" + fileName + ", λ=" +  String.format("%.2f", (double)ADDITIONAL_TASK_NUM/TASK_ADDITION_SPAN)+ ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);
            // 列番号入れる部分
            pw.print("id");
            for (int i = 0; i < AGENT_NUM; i++) pw.print(", " + i);
            pw.println();
            for (Agent ag : agents) {
                pw.print(ag.id + ", ");
                if (ag.e_member > ag.e_leader) {
                    for (int i = 0; i < AGENT_NUM; i++) {
                        pw.print("-" + ag.reliabilities[i] + ", ");
                    }
                } else {
                    for (int i = 0; i < AGENT_NUM; i++) {
                        pw.print(ag.reliabilities[i] + ", ");
                    }
                }
                pw.println();
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
// */

/*        pw.print("I'm 4 "+ ", ");
        for (int i = 0; i < AGENT_NUM; i++) pw.print(agents.get(4).reliabilities[i] + ", ");
        pw.println();
// */
    }

    static int countReciplocalist(List<Agent> agents) {
        int temp = 0;
/*        for (Agent agent : agents) {
            if( agent.principle == RATIONAL ) temp++;
        }
// */
        for (Agent agent : agents) {
            if (agent.reliabilities[agent.relRanking.get(0).id] > THRESHOLD_FOR_DEPENDABILITY && (agent.e_member > THRESHOLD_FOR_RECIPROCITY || agent.e_leader > THRESHOLD_FOR_RECIPROCITY))
                temp++;
        }
        return temp;
    }

    static int[] dCounts = new int[MAX_DELAY + 1];

    static void countDelays(int[][] delays) {
        for (int[] row : delays) {
            for (int column : row) {
                dCounts[column]++;
            }
        }
    }

    static int countReciplocalMembers(List<Agent> agents) {
        int temp = 0;
/*        for (Agent agent : agents) {
            if( agent.principle == RATIONAL ) temp++;
        }
// */
        for (Agent agent : agents) {
            if (agent.reliabilities[agent.relRanking.get(0).id] > THRESHOLD_FOR_DEPENDABILITY && agent.e_member > THRESHOLD_FOR_RECIPROCITY && agent.e_member > agent.e_leader)
                temp++;
        }
        return temp;
    }

    private static void setBorder(CellStyle style, BorderStyle border) {
        style.setBorderBottom(border);
        style.setBorderTop(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
    }

    private final static String[] LIST_ALPHA = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    private String setPath( String dir_name, String file_name ){
        String currentPath = System.getProperty("user.dir");
<<<<<<< HEAD
        String outputFilePath = currentPath + "/out/" + dir_name + "/" + file_name + ",λ=" + (double)ADDITIONAL_TASK_NUM/TASK_ADDITION_SPAN + ".xlsx";
=======
        String outputFilePath = currentPath + "/out/" + dir_name + "/" + file_name + ",λ=" +  String.format("%.2f", (double)ADDITIONAL_TASK_NUM/TASK_ADDITION_SPAN) + ".xlsx";
>>>>>>> CNP
        return outputFilePath;
    }

    private void prepareExcelSheet(  ){

    }

    // あるrowのcolumn列にoを書き込むメソッド
    // 返り値は
    private Cell writeCell( Row row, int col_number, CellStyle style, Object o){
        Cell cell;
        if( o.getClass().getName() == "java.lang.String" ){
            cell = row.createCell(col_number++);
            cell.setCellStyle(style);
            cell.setCellType(CellType.STRING);
            cell.setCellValue( o.toString() );
        }else if( o.getClass().getName() == "java.lang.Integer" ){
            cell = row.createCell(col_number++);
            cell.setCellStyle(style);
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue( (int) o );
        }else{
            cell = row.createCell(col_number++);
            cell.setCellStyle(style);
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue( (double) o );
        }
        return cell;
    }

    private static String getExcelColumnString(int column) {
        String result = "";

        if (column >= 0) {
            if (column / OutPut.LIST_ALPHA.length > 0) {
                result += getExcelColumnString(column / OutPut.LIST_ALPHA.length - 1);
            }
            result += OutPut.LIST_ALPHA[column % OutPut.LIST_ALPHA.length];
        }

        return result;
    }
}
