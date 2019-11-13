package main.research.agent.strategy.reliableAgents;

import main.research.Manager;
import main.research.agent.Agent;
import main.research.agent.strategy.OCTuple;
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

class OCTupleTest {
	List< OCTuple > ocTupleList = new ArrayList<>(  );
	int currentTime = 500;

	static {
		System.out.println( "OCSetTest" );
	}

	void setUp() throws NoSuchFieldException, IllegalAccessException {
		List< Agent > agentList = Initiation.getNewAgentList();
		for( Agent ag : agentList ) {
		}
		Collections.sort( ocTupleList, Comparator.comparingInt( OCTuple::getLastUpdatedTime ) );

		Field field = Manager.class.getDeclaredField( "turn" );
        field.setAccessible(true);
		field.set( Manager.class, currentTime); // 設定
	}

	@Test
	void refreshMapで期限切れのものが残らない() {
		int before = ocTupleList.size();

		assertThat( before, is( greaterThan( ocTupleList.size() ) ) );
		for( OCTuple set: ocTupleList ) {
			int timeElapsed = Manager.getCurrentTime() - set.getLastUpdatedTime();
			assertThat( timeElapsed, is( lessThan( OC_CACHE_TIME ) ) );
		}
	}
}