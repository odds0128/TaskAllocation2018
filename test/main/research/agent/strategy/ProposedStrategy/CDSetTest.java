package main.research.agent.strategy.ProposedStrategy;

import main.research.Manager;
import main.research.agent.Agent;
import main.research.others.random.MyRandom;
import main.research.util.Initiation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import static main.research.SetParam.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

class CDSetTest {
	List<CDSet> cdSetList = new ArrayList<>(  );
	int currentTime = 500;

	static {
		System.out.println( "CDSetTest" );
	}

	@BeforeEach
	void setUp() throws NoSuchFieldException, IllegalAccessException {
		List< Agent > agentList = Initiation.getNewAgentList();
		for( Agent ag : agentList ) {
			cdSetList.add( new CDSet( ag, new double[RESOURCE_TYPES], MyRandom.getRandomInt( 0, currentTime - 1 ) ) );
		}
		Collections.sort( cdSetList, Comparator.comparingInt( CDSet::getLastUpdatedTime ) );

		Field field = Manager.class.getDeclaredField( "turn" );
        field.setAccessible(true);
		field.set( Manager.class, currentTime); // 設定
	}

	@Test
	void refreshMapで期限切れのものが残らない() {
		int before = cdSetList.size();
		CDSet.refreshMap( cdSetList );

		assertThat( before, is( greaterThan( cdSetList.size() ) ) );
		for( CDSet set: cdSetList ) {
			int timeElapsed = Manager.getCurrentTime() - set.getLastUpdatedTime();
			assertThat( timeElapsed, is( lessThan( CD_CACHE_TIME ) ) );
		}
	}
}