package main.research.agent.strategy.ProposedStrategy;

import main.research.Manager;
import main.research.agent.Agent;
import main.research.others.random.MyRandom;
import main.research.task.Subtask;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static main.research.SetParam.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ProposedStrategy_lTest {
	static {
		System.out.println( "ProposedStrategy_l test" );
	}

	static List< CDSet > cdSetList = new ArrayList<>(  );
	static double[][] cdList = {
		{ 0, 0, 3 }, { 0, 0, 2.5 }, { 0, 0, 5 }, { 1, 0, 0 }, { 2, 0, 5 },
		{ 0, 3, 4 }, { 4, 2.4, 4 }, { 2, 0, 0 }, { 3, 3, 3 }, { 0, 5, 2 }
	};
	static double[] deList = {
		0.5, 0.4, 0.3, 0.7, 0.6, 0.8, 0.2, 0.3, 0.6, 0.8
	};
	static int cdSetListSize = cdList.length;
	static int currentTime = 500;
	static Method targetMethod;
	static Agent   leaderMock = Mockito.mock( Agent.class );
	static Subtask stMock     = Mockito.mock( Subtask.class );

	@BeforeAll
	static void setUp() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException {
		Constructor< Subtask > constructor = Subtask.class.getDeclaredConstructor();
		constructor.setAccessible( true );

		stMock.resType = 2;
		leaderMock.ls = new ProposedStrategy_l();
		for ( int i = 0; i < cdSetListSize; i++ ) {
			Agent mockAgent = Mockito.mock( Agent.class );
			mockAgent.id = i;
			mockAgent.resources = new int[RESOURCE_TYPES];
			for( int j = 0; j < RESOURCE_TYPES; j++ ) {
				mockAgent.resources[j] = ( int ) cdList[i][j];
			}
			leaderMock.ls.reliableMembersRanking.put( mockAgent, deList[i] );
			cdSetList.add( new CDSet( mockAgent, cdList[ i ], MyRandom.getRandomInt( currentTime - CD_CACHE_TIME, currentTime ) ) );
		}
		targetMethod = ProposedStrategy_l.class.getDeclaredMethod( "selectAMemberForASubtask", Subtask.class );
		targetMethod.setAccessible( true );

		Field field;
		field= ProposedStrategy_l.class.getDeclaredField( "CDList" );
		field.setAccessible( true );
		field.set( leaderMock.ls, cdSetList );

		field = Manager.class.getDeclaredField( "turn" );
		field.setAccessible( true );
		field.set( Manager.class, currentTime );
	}

	@Nested
	class selectAMemberForASubtaskのテスト {
		@Test
		void 選択されたエージェントがそのサブタスクを実行することができる() throws InvocationTargetException, IllegalAccessException {
			Agent ag = ( Agent ) targetMethod.invoke( leaderMock.ls, stMock );

			boolean actual   = ag.resources[stMock.resType] > 0;
			boolean expected = true;
			assertThat( actual, is( expected ) );
		}

		@Test
		void 最もCDが高いエージェントを選択できる() throws InvocationTargetException, IllegalAccessException {
			Agent ag = ( Agent ) targetMethod.invoke( leaderMock.ls, stMock );

			double[] actual   = CDSet.getCD( ag, cdSetList );
			double expected = 5.0;
			assertThat( actual[stMock.resType], is( expected ) );
		}

		@Test
		void 最もCDが高いエージェントの中でも最もDEが高いエージェントを選択できる() throws InvocationTargetException, IllegalAccessException {
			Agent ag = ( Agent ) targetMethod.invoke( leaderMock.ls, stMock );

			double actual   = leaderMock.ls.reliableMembersRanking.get( ag );
			double expected =  0.6;
			assertThat( actual, is( expected ) );
		}
	}
}