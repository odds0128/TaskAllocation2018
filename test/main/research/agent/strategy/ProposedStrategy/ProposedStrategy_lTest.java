package main.research.agent.strategy.ProposedStrategy;

import main.research.Manager;
import main.research.agent.Agent;
import main.research.others.random.MyRandom;
import main.research.util.Initiation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static main.research.SetParam.MAX_AGENT_RESOURCE_SIZE;
import static main.research.SetParam.RESOURCE_TYPES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ProposedStrategy_lTest {
	static List< Agent > agentList;

	static {
		System.out.println( "ProposedStrategy_l test" );
	}

	@BeforeAll
	static void setUp() {
		agentList = Initiation.getNewAgentList();
	}

	@Nested
	class selectAMemberForASubtaskのテスト {
		List<CDSet> cdSetList;
		int currentTime = 500;

		@BeforeEach
		void setUp() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
			for( Agent ag : agentList ) {
				cdSetList.add( new CDSet( ag, null, MyRandom.getRandomInt( 0, currentTime ) ) );
			}
			CDSet.refreshMap( cdSetList );
			setCD();

			Field field = Manager.class.getDeclaredField( "turn" );
			field.setAccessible( true );
			field.set( Manager.class, currentTime);
		}

		@Test
		void 最もCDが高いエージェントを選択できる() {
//			double actual = 一位のエージェントのCD;
		}

		@Test
		void 選択されたエージェントがそのサブタスクを実行することができる() {

		}

		void setCD() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
			Method method = CDSet.class.getDeclaredMethod("setCongestionDegree", double[].class );
			method.setAccessible(true);

			for( CDSet entry: cdSetList ) {
				double[] resourceArray = new double[ RESOURCE_TYPES ];
				int resType = MyRandom.getRandomInt( 0, RESOURCE_TYPES - 1 );
				int resSize = MyRandom.getRandomInt( 1, MAX_AGENT_RESOURCE_SIZE );
				resourceArray[ resType ] = resSize;

				method.invoke( entry, resourceArray );
			}

		}
	}

}