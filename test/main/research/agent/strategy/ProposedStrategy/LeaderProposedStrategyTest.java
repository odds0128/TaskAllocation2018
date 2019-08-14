package main.research.agent.strategy.ProposedStrategy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import main.research.Manager;
import main.research.agent.Agent;


import org.junit.jupiter.api.*;

import static org.hamcrest.Matchers.*;

import java.lang.reflect.Method;

import static main.research.SetParam.RESOURCE_TYPES;
import static main.research.random.MyRandom.getRandomInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

class LeaderProposedStrategyTest {
	@Nested
	class refreshResourceCacheのテスト {
		ProposedStrategy_l ls = new ProposedStrategy_l();
		Method rrc;

		@BeforeEach
		void setUp() throws NoSuchMethodException {
			// テスト対象privateメソッドのリフレクション
			rrc = ProposedStrategy_l.class.getDeclaredMethod( "refreshResourceCache");
			rrc.setAccessible(true);

			// resourceCacheモックの作成
			Agent mockAgent = mock(Agent.class);
			ls.teamHistoryCache.add( new TeamHistoryCache( 5, mockAgent, getRandomInt(0, RESOURCE_TYPES - 1), getRandomInt(5, 20) ) );
			ls.teamHistoryCache.add( new TeamHistoryCache( 9899, mockAgent, getRandomInt(0, RESOURCE_TYPES - 1), getRandomInt(5, 20) ) );
			ls.teamHistoryCache.add( new TeamHistoryCache( 9899, mockAgent, getRandomInt(0, RESOURCE_TYPES - 1), getRandomInt(5, 20) ) );
			ls.teamHistoryCache.add( new TeamHistoryCache( 9900, mockAgent, getRandomInt(0, RESOURCE_TYPES - 1), getRandomInt(5, 20) ) );
			ls.teamHistoryCache.add( new TeamHistoryCache( 9950, mockAgent, getRandomInt(0, RESOURCE_TYPES - 1), getRandomInt(5, 20) ) );
		}

		@Test
		void 現在時刻10000ticksで9899ticksにおける履歴が消える(){
			// 現在時刻を10000ticksに設定
			Manager manager = new Manager();
			Field targetField = null;     // 更新対象アクセス用のFieldオブジェクトを取得する。
			try {
				targetField = manager.getClass().getDeclaredField("turn");
				targetField.setAccessible(true);
				targetField.set(null, 10000);
			} catch (IllegalAccessException | NoSuchFieldException e) {
				e.printStackTrace();
			}

			try {
				rrc.invoke( ls );
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
			assertThat( include9899(ls.teamHistoryCache), is( not(true)) );
		}

		boolean include9899( List<TeamHistoryCache> list ) {
			for( TeamHistoryCache unit : list ){
				System.out.println( unit );
				if( unit.getCachedTime() <= 9899 ) {
					return true;
				}
			}
			return false;
		}

		@AfterEach
		void tearDown(){
			ls.teamHistoryCache.clear();
		}
	}
}