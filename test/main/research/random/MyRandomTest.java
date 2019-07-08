package main.research.random;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.closeTo;


class MyRandomTest {

    @Nested
    class newSfmtのテスト {
        @Test
        void newSfmtで同じ引数からは同じSfmtオブジェクトが返ってくる() {
            for (int i = 0; i < 100; i++) {
                Sfmt expected = MyRandom.newSfmt(i);
                Sfmt actual = MyRandom.newSfmt(i);

                /*
                 Sfmtオブジェクトの等価性がわからなかったので生成される
                 乱数の値で比較することにする
                */
                for (int j = 0; j < 100; j++) {
                    assertThat( actual.NextInt(100), is( equalTo(expected.NextInt(100) ) ) );
                }

            }
        }

        @Test
        void newSfmtで違う引数からは違うSfmtオブジェクトが返ってくる() {
            for (int i = 0; i < 100; i++) {
                Sfmt expected = MyRandom.newSfmt(i);
                Sfmt actual = MyRandom.newSfmt(i+1);

                /*
                 Sfmtオブジェクトの等価性がわからなかったので生成される
                 乱数の値で比較することにする
                */
                boolean isDifferent = false;
                for (int j = 0; j < 100; j++) {
                    if ( actual.NextInt(100) != expected.NextInt(100) ) {
                        isDifferent = true;
                        break;
                    }
                }
                assertThat( isDifferent, is(true) );
            }
        }
    }


    @Nested
    class getRandomIntのテスト {
        Sfmt rnd;
        int actual;
        int max;
        int min;


        @BeforeEach
        void Setup(){
            rnd = MyRandom.newSfmt( 0 );
            max = 9;
            min = 3;
        }

        @Test
        void getRandomIntで生成される乱数がmin以上max以下におさまる() {
            for (int i = 0; i < 100; i++) {
                actual = MyRandom.getRandomInt(min, max);
                System.out.println(actual);
                assertThat(actual, is( both( greaterThanOrEqualTo(min) ).and( lessThanOrEqualTo(max) ) ) );
            }
        }

        @Test
        void getRandomIntでminである乱数が生成されることがある() {
            boolean minAppeared = false;
            for (int i = 0; i < 100; i++) {
                actual = MyRandom.getRandomInt(min, max);
                if ( actual == min ) {
                    minAppeared = true;
                    break;
                }
            }
            assertThat( minAppeared, is(true) );
        }

        @Test
        void getRandomIntでmaxである乱数が生成されることがある() {
            boolean maxAppeared = false;
            for (int i = 0; i < 100; i++) {
                actual = MyRandom.getRandomInt(min, max);
                if ( actual == max ) {
                    maxAppeared = true;
                    break;
                }
            }
            assertThat( maxAppeared, is(true) );
        }

        @Test
        void getRandomIntで均等に乱数が生成される() {
            int patterns = max - min + 1 ;
            double[] freq  = new double[patterns];
            int iterate = 1000;
            double expected = 1.0 / patterns;
            double acceptable_errors = expected * 0.1;

            int actual;
            for (int i = 0; i < iterate; i++) {
                actual = MyRandom.getRandomInt(min, max);
                freq[actual - min] ++;
            }

            for (int i = 0; i < patterns; i++) {
                assertThat( freq[i] / iterate , is( closeTo( expected, acceptable_errors ) ) );
            }
        }
    }
}