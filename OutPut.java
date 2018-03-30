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

    static OutPut _syngleton = new OutPut();

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
        /*        disposedTasksArray[index] += dt;
        overflownTasksArray[index] += ot;
        messagesArray[index] += TransmissionPath.messageNum;
        reciprocalistsArray[index] += Agent._recipro_num;
        rationalistsArray[index] += Agent._rational_num;
        reciprocalMembersArray[index] += rm;
        finishedTasksInDepopulatedAreaArray[index] += ftida ;
        finishedTasksInPopulatedAreaArray[index]   += ftipa ;
// */
    }
    static void indexIncrement() {
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
            System.out.println(temp.subTasks);
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
        for (Allocation al : leader.allocations) {
            System.out.println(" member: " + al.getCandidate() + "→" + al.getSubtask() + ": " + leader.calcExecutionTime(al.getCandidate(), al.getSubtask()) + "[tick(s)]");
        }
    }

    static void showLeaderRetirement(List<Agent> snapshot, List<Agent> agents) {
        int countPositiveBefore, countPositiveAfter;
        int mCountPositiveBefore = 0, mCountPositiveAfter = 0;
        Agent ag;
        for (Agent ss : snapshot) {
            ag = agents.get(ss.id);
            // まず, メンバになったのかどうか確認. なっていたら, その理由を究明
            if (ag.e_leader < ag.e_member) {
                countPositiveBefore = 0;
                countPositiveAfter = 0;
                for (double rel : ag.reliabilities) {
                    if (rel > 0) countPositiveAfter++;
                }
                mCountPositiveBefore += countPositiveBefore;
                mCountPositiveAfter += countPositiveAfter;
                System.out.println("ID: " + ss.id
                        + ", Positive Agents: " + countPositiveAfter
                );
            }
        }
        int countPositive, countLeader = 0;
        int mCountPositive = 0;
        System.out.println("Normal leaders↓");
        for (Agent nAg : agents) {
            if (nAg.e_leader > nAg.e_member) {
                countLeader++;
                countPositive = 0;
                for (double rel : nAg.reliabilities) {
                    if (rel > 0) countPositive++;
                }
                mCountPositive += countPositive;
                System.out.println("ID: " + nAg.id + ", el: "
                        + ", Positive Agents: " + countPositive
                );
            }
        }
        System.out.println("loser:  → " + mCountPositiveAfter / snapshot.size());
        System.out.println("Survivor: " + mCountPositive / countLeader);
    }
    static void showResults(int turn, List<Agent> agents, int num) {
        int tempL = 0, tempM = 0;
        int neetL = 0, neetM = 0;
        System.out.println(" Turn: " + turn);
        System.out.println("  Total Agents is " + Agent._id);
        int temp = countReciplocalist(agents);
        for (Agent ag : agents) {
            if (ag.e_leader > ag.e_member) {
                tempL++;
                // 最後に活動したのがだいぶ前のことであるならば
                if (MAX_TURN_NUM - ag.validatedTicks > THRESHOLD_FOR_NEET) neetL++;
            } else if (ag.e_member > ag.e_leader) {
                tempM++;
                if (MAX_TURN_NUM - ag.validatedTicks > THRESHOLD_FOR_NEET) {
                    neetM++;
                    System.out.println(ag.validatedTicks + ", " + ag.e_leader + ", " + ag.e_member);
                }
            }
        }
        System.out.println("  Leaders is " + tempL + ", Members is " + tempM
                + " (Rationalists: " + (AGENT_NUM - temp) + ", Reciprocalists: " + (temp) + "), "
                + " (Reciplocal member: " + countReciplocalMembers(agents) + " )"
                + "NEET l: " + neetL + ", m: " + neetM);
        System.out.println("  Messages  : " + TransmissionPath.messageNum
                + ", Mean TransmitTime: " + String.format("%.2f", communicationDelayArray[WRITING_TIMES - 1] / num)
                + ", Proposals: " + TransmissionPath.proposals + ", Replies: " + TransmissionPath.replies
                + ", Acceptances: " + TransmissionPath.acceptances + ", Rejects: " + TransmissionPath.rejects);
        System.out.println("   , Results: " + TransmissionPath.results
                + ", Finished: " + TransmissionPath.finished + ", Failed:" + TransmissionPath.failed);
// */
    }

    static void writeGraphInformation(List<Agent> agents, String fp) throws FileNotFoundException, IOException {
        String currentPath  = System.getProperty("user.dir");
        String outputFilePath = currentPath + "/out/実験結果/" + fp + ".xlsx";
        Edge edge = new Edge();
        edge.makeEdge(agents);
//        System.exit(0);
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

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" leader id");

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" delay to leader ");

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" is lonely or not");

                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" is accompanied or not");
                } else {
                    cell = row.createCell(colNumber++);
                    cell.setCellStyle(style_header);
                    cell.setCellType(CellType.STRING);
                    cell.setCellValue(" length ");
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

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        if (agent.e_leader > agent.e_member) cell.setCellValue(agent.id * 3);
                        else if (agent.leader != null) cell.setCellValue(agent.leader.id * 3);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        if (agent.e_leader > agent.e_member) cell.setCellValue(0);
                        else if (agent.leader != null)
                            cell.setCellValue(Manager.delays[agent.id][agent.leader.id] * 10);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.isLonely);

                        cell = row.createCell(colNumber++);
                        cell.setCellStyle(style_string);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(agent.isAccompanied);

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
    static void writeResults(Strategy st) {
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        String fileName = st.getClass().getName();
        try {
            String currentPath  = System.getProperty("user.dir");
            fw = new FileWriter(currentPath + "/out/実験結果/" + fileName + ", λ=" + ADDITIONAL_TASK_NUM + ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            pw.println("turn" + ", "
                            + "FinishedTasks" + ", " //+ "DisposedTasks"                     + ", "+ "OverflownTasks"                    + ", "
                            + "CommunicationTime" + ", "
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
                                + communicationDelayArray[i] / (double) EXECUTION_TIMES + ", "
                                + leaderNumArray[i] / EXECUTION_TIMES + ", "
                                + neetMembersArray[i] / EXECUTION_TIMES + ", "
/*                    + disposedTasksArray[i]                  / EXECUTION_TIMES + ", "
                    + overflownTasksArray[i]                 / EXECUTION_TIMES + ", "
                    + memberNumArray[i]                      / EXECUTION_TIMES + ", "
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
    static void writeDelays(int[][] delays){
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        try {
            String currentPath  = System.getProperty("user.dir");
            fw = new FileWriter(currentPath + "/out/実験結果/communicationDelay=" + MAX_DELAY + ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);
            pw.println("delay" + ", " + " count ");
            for( int i = 1 ; i < MAX_DELAY + 1 ; i++ ){
                pw.println(i + ", " + dCounts[i]/2);
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }
    static void writeReliabilities(int turn, List<Agent> agents, Strategy st) {
        FileWriter fw;
        BufferedWriter bw;
        PrintWriter pw;
        String fileName = st.getClass().getName();
        try {
            fw = new FileWriter("/Users/r.funato/IdeaProjects/TaskAllocation/src/r" + fileName + ", λ=" + ADDITIONAL_TASK_NUM + ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);
            // 列番号入れる部分
//        for (int i = 0; i < AGENT_NUM; i++) pw.print(", " + i);
//        pw.println();
            for (Agent ag : agents) {
                pw.print("I'm " + ag.id + ", ");
                for (int i = 0; i < AGENT_NUM; i++) {
                    if (ag.e_member > ag.e_leader) {
                        pw.print(ag.reliabilities[i] + ", ");
                    } else {
                        pw.print("-" + ag.reliabilities[i] + ", ");
                    }
                }
                pw.println();
            }
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
    static void countDelays(int[][] delays){
        for( int[] row : delays ){
            for( int column : row ){
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
