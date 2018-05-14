import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CNP_area_restricted implements SetParam, Strategy {
    static final int reception_span = 3;

    int[] reception_time = new int[AGENT_NUM];


    CNP_area_restricted() {
        for (int i = 0; i < AGENT_NUM; i++) {
            reception_time[i] = reception_span;
        }
    }

    public void actAsLeader(Agent agent) {
        if (agent.phase == lPHASE1) publicize(agent);
        else if (agent.phase == lPHASE2) organize(agent);
        else if (agent.phase == PHASE3) execute(agent);
    }

    public void actAsMember(Agent agent) {
        if (agent.id == 2 && Manager.getTicks() > 145) {
            int i = 0;
        }

        if (agent.phase == mPHASE1) bid(agent);
        else if (agent.phase == mPHASE2) waitResult(agent);
        else if (agent.phase == PHASE3) execute(agent);
    }

    // publicizeメソッド ... 近い方から約100体のエージェントに広報する
    private void publicize(Agent le) {
        le.ourTask = Manager.getTask();
        if (le.ourTask == null) return;
        le.selectSubTask();
        for (Agent ag : le.canReach) {
            TransmissionPath.sendMessage(new Message(le, ag, PUBLICITY, le.ourTask, null));
        }
//        System.out.println("ID: " + le.id + " published " + le.ourTask);
        this.nextPhase(le);
    }

    // bidメソッド ... リーダーに入札する
    private void bid(Agent mem) {
        reception_time[mem.id]--;
        if (reception_time[mem.id] == 0) {
            // 受付時間が終了したら各処置をして受付時間を戻す
            if (mem.messages.size() == 0) {
                reception_time[mem.id] = reception_span;
                return;
            } else {
                mem.leader = selectLeader(mem, mem.messages);
//                System.out.println("ID: " + mem.id + " is bidding to " + mem.leader.id);
                reception_time[mem.id] = reception_span;
                if (mem.leader.equals(mem)) return;
                else this.nextPhase(mem);
            }
        }
    }

    // organizeメソッド ... 入札者の中から落札者を選びチームを編成する
    private void organize(Agent le) {
        // 入札・非入札メッセージが全員から返って来たか確認する
        if (le.messages.size() != le.canReach.size()) return;

        // 全員分のメッセージが確認できたら割り当てを考える
        // 割り当ては，実行時間が短い方を優先する
        // 下の配列はそれぞれ添え字がサブタスクのインデックスに対応する
        // そのサブタスクを割り当てる暫定のエージェントを格納する配列とその実行時間を格納する配列
        Agent[] bestAllocations = new Agent[le.restSubTask];
        int[] bestEstimations = new int[le.restSubTask];
        for (int i = 0; i < le.restSubTask; i++) {
            bestEstimations[i] = Integer.MAX_VALUE;
            bestAllocations[i] = le;
        }

        int size = le.messages.size();
        for (int i = 0; i < size; i++) {
            Message m = le.messages.remove(0);

            if (m.getBidStIndex() >= 0) {
                // 暫定の一位よりも早く終わりそうなら

                if (m.getEstimation() < bestEstimations[m.getBidStIndex()]) {
                    // 元一位に未割り当てのメッセージを送って暫定一位を入れ替える
                    TransmissionPath.sendMessage(new Message(le, bestAllocations[m.getBidStIndex()], BID_RESULT, null, null));
                    bestEstimations[m.getBidStIndex()] = m.getEstimation();
                    bestAllocations[m.getBidStIndex()] = m.getFrom();
                }
                // そもそも暫定一位より遅ければ未割り当てメッセージを送る
                else {
                    TransmissionPath.sendMessage(new Message(le, m.getFrom(), BID_RESULT, null, null));
                }
            }
            // 非入札メッセージは無視する
        }

        // 割り当てが全部埋まったか確認する
        for (Agent al : bestAllocations) {
            // 一つでも割当先が見つからなければ失敗と判断し，割り当て候補にも未割り当てを送信するとともに
            // タスクを破棄して次のステップに行く
            if (al == le) {
                Manager.disposeTask(le);
                for (Agent allo : bestAllocations) {
                    TransmissionPath.sendMessage(new Message(le, allo, BID_RESULT, null, null));
                }
                this.inactivate(le, 0);
                return;
            }
        }
        // 割り当てが決まったらそいつらに実際にサブタスクを割り当てる
        for (int i = 0; i < le.restSubTask; i++) {
            le.teamMembers.add(bestAllocations[i]);
            le.preAllocations.put(bestAllocations[i], le.ourTask.subTasks.get(i));
            TransmissionPath.sendMessage(new Message(le, bestAllocations[i], BID_RESULT, le.ourTask.subTasks.get(i), null));
        }
        Manager.finishTask(le);
//        System.out.println("ID: " + le.id + " with " + le.teamMembers);
        this.nextPhase(le);
    }

    // waitResultメソッド ... 入札がどうなったかを待つメソッド
    private void waitResult(Agent mem) {
        // リーダーからの返事が来るまで待つ
        if (mem.messages.size() == 0) return;

        Message message;
        message = mem.messages.remove(0);
        mem.mySubTask = message.getSt();

        // サブタスクがもらえたなら実行フェイズへ移る.
        if (mem.mySubTask != null) {
            mem.executionTime = mem.calcExecutionTime(mem, mem.mySubTask);
            mem.allocated[mem.leader.id][mem.mySubTask.resType]++;
            //      System.out.println("ID:" + mem.id + "'s subtask will be executed in " + mem.executionTime);
            this.nextPhase(mem);
        }
        // サブタスクが割り当てられなかったら, inactivate
        else {
            this.inactivate(mem, 0);
        }
    }

    // executeメソッド ... 落札者とリーダーは自分の担当するサブタスクを実行する
    private void execute(Agent ag) {
        ag.executionTime--;
        ag.validatedTicks = Manager.getTicks();

        if (ag.executionTime <= 0) {
            if (ag.role == LEADER) {
                if (ag._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
                    for (Agent agent : ag.teamMembers) ag.workWithAsL[agent.id]++;
                }
                this.inactivate(ag, 1);
            } else {
                if (ag._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
                    ag.workWithAsM[ag.leader.id]++;
                }
                ag.sendMessage(ag, ag.leader, DONE, 0);
                ag.required[ag.mySubTask.resType]++;
                // 自分のサブタスクが終わったら非活性状態へ
                this.inactivate(ag, 1);
            }
        }
    }

    public List<Agent> selectMembers(Agent agent, List<SubTask> subtasks) {
        return null;
    }

    public Agent selectLeader(Agent mem, List<Message> messages) {
        int size = mem.messages.size();
        double maxMaxReward = 0;
        double tempMaxReward;
        Agent bidAg = mem; // 暫定の入札対象者　初期値は自分
        int bidStIndex = -1; // 暫定の入札対象サブタスク
        int minRestraint = 0;

        for (int i = 0; i < size; i++) {
            Message m = mem.messages.remove(0);

            int tempStIndex = selectBestSt(mem, m.getBidTask());

            if (tempStIndex < 0) {
                TransmissionPath.sendMessage(new Message(mem, m.getFrom(), BIDDINGorNOT, -1, 0));
                continue;
            }

            // 今見てるメッセージのサブタスク中で一番やりたいやつと，
            // 今まで見てきた中で一番やりたいやつの報酬を比較し，
            // いい方を残す
            SubTask tempSt = m.getBidTask().subTasks.get(tempStIndex);
            tempMaxReward = (double) tempSt.reqRes[tempSt.resType] / mem.calcExecutionTime(mem, tempSt);

            if (maxMaxReward < tempMaxReward) {
                TransmissionPath.sendMessage(new Message(mem, bidAg, BIDDINGorNOT, -1, 0));
                maxMaxReward = tempMaxReward;
                minRestraint = mem.calcExecutionTime(mem, tempSt);
                bidStIndex = tempStIndex;
                bidAg = m.getFrom();
            } else {
                TransmissionPath.sendMessage(new Message(mem, m.getFrom(), BIDDINGorNOT, -1, 0));
            }
        }

//        System.out.println("ID: " + mem.id + " bid " + bidAg.id);
        TransmissionPath.sendMessage(new Message(mem, bidAg, BIDDINGorNOT, bidStIndex, minRestraint));
        return bidAg;
    }

    public void checkMessages(Agent self) {
        // 現状，リーダーの広報時に自分からのメッセージが到着することがあり得ることに注意
        Message m;
        int size = self.messages.size();
        // リーダーは

        if (self.role == LEADER) {
            if (self.phase == lPHASE1 || self.phase == PHASE3) {
                // 広報時とタスク実行時は何も待ってない
                for (int i = 0; i < size; i++) {
                    m = self.messages.remove(0);
                    // mが広報メッセージなら非入札メッセージを送る．
                    assert m.getMessageType() == PUBLICITY || m.getMessageType() == DONE : "広報と終了報告以外はありえないはず";
                    if (m.getMessageType() == PUBLICITY)
                        TransmissionPath.sendMessage(new Message(self, m.getFrom(), BIDDINGorNOT, -1, 0));
                }
            } else if (self.phase == lPHASE2) {
                // 全員からの入札・非入札メッセージを待っている．広報は拒否．それ以外が来ることはないはず．
                for (int i = 0; i < size; i++) {
                    m = self.messages.remove(0);
                    assert (m.getMessageType() == PUBLICITY || m.getMessageType() == BIDDINGorNOT || m.getMessageType() == DONE ) : "広報か(非)入札か終了報告以外はありえない";
                    if (m.getMessageType() == PUBLICITY) {
                        TransmissionPath.sendMessage(new Message(self, m.getFrom(), BIDDINGorNOT, -1, 0));
                    } else if (m.getMessageType() == BIDDINGorNOT) {
                        self.messages.add(m);
                    }
//                    if( m.getBidStIndex() != 0 ) System.out.println("ID: " + self.id + " Bidden by " + m.getFrom());
                }
            }
        }
        // メンバは
        else if (self.role == MEMBER) {
            if (self.phase == mPHASE1) {
                // リーダーからの広報を待っている．広報以外は来ないはず．ただ，ほんとに来んのならここいらない．
                for (int i = 0; i < size; i++) {
                    m = self.messages.remove(0);
                    assert (m.getMessageType() == PUBLICITY) : "広報以外はありえない";
                    if (m.getMessageType() == PUBLICITY) self.messages.add(m);
                }
            } else if (self.phase == mPHASE2) {
                // 入札に対する結果を待っている
                for (int i = 0; i < size; i++) {
                    m = self.messages.remove(0);
//                    if( m.getFrom().id == 379 && m.getTo().id == 2 && Manager.getTicks() > 130 ){
//                        System.out.println();
//                    }

                    assert (m.getMessageType() == PUBLICITY || m.getMessageType() == BID_RESULT) : "広報か入札に対する結果以外はありえない";
                    if (m.getMessageType() == PUBLICITY)
                        TransmissionPath.sendMessage(new Message(self, m.getFrom(), BIDDINGorNOT, -1, 0));
                    else {
                        self.messages.add(m);
                    }
                }
            } else if (self.phase == PHASE3) {
                // 実行時は何も待っていない
                for (int i = 0; i < size; i++) {
                    m = self.messages.remove(0);
                    assert (m.getMessageType() == PUBLICITY) : "広報以外はありえない";
                    if (m.getMessageType() == PUBLICITY)
                        TransmissionPath.sendMessage(new Message(self, m.getFrom(), BIDDINGorNOT, -1, 0));
                }
            }
        }
    }

    private int selectBestSt(Agent ag, Task t) {
        // 今見ているタスクの中で単位時間あたりの報酬が最も大きいものを選ぶ
        int stIndex = -1;
        double maxValue = 0;
        double tempValue;

        for (int i = 0; i < t.subTaskNum; i++) {
            SubTask st = t.subTasks.get(i);
            tempValue = (double) st.reqRes[st.resType] / ag.calcExecutionTime(ag, st);

            if (maxValue < tempValue) {
                maxValue = tempValue;
                stIndex = i;
            }
        }
        return stIndex;
    }

    protected void nextPhase(Agent ag) {
        ag.validatedTicks = Manager.getTicks();
        if (ag.role == LEADER) {
            if (ag.phase == lPHASE1) {
                ag.phase = lPHASE2;
            } else if (ag.phase == lPHASE2) {
                ag.phase = PHASE3;
            }
        } else {
            if (ag.phase == mPHASE1) {
                ag.phase = mPHASE2;
            } else if (ag.phase == mPHASE2) {
                ag.phase = PHASE3;
            }
        }
    }

    void inactivate(Agent ag, int success) {
        if (ag.role == LEADER) {
            ag.phase = lPHASE1;
            ag.teamMembers.clear();        // すでにサブタスクを送っていてメンバの選定から外すエージェントのリスト
            ag.ourTask = null;
            ag.preAllocations.clear();
        } else if (ag.role == MEMBER) {
            ag.phase = mPHASE1;
        }
        ag.mySubTask = null;
        ag.messages.clear();
        ag.executionTime = 0;
        ag.validatedTicks = Manager.getTicks();
        ag.leader = null;
    }


    public void clearStrategy() {
        for (int i = 0; i < AGENT_NUM; i++) {
            reception_time[i] = reception_span;
        }
    }
}

