package org.bbs.android.bluzloopback;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by bysong on 16-3-14.
 */
@RunWith(AndroidJUnit4.class)
public class ConversationActivityTest extends ActivityInstrumentationTestCase2<ConversationActivity> {
    private ConversationActivity mActivity;

//    public FocusActivityTest(Class<FocusActivity> activityClass) {
//        super(activityClass);
//    }

    public ConversationActivityTest(){
        super(ConversationActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Injecting the Instrumentation instance is required
        // for your test to run with AndroidJUnitRunner.
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mActivity = getActivity();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testFocusPath(){
        View v = mActivity.getWindow().getDecorView();

    }


    public static void assertIdIs(View view, int id)
    {
        String expectStr = viewStr(view);
        String actualStr = viewStr(getRootView(view));
        assertEquals("expected:" + expectStr + " but actual: " + actualStr , view.getId(), id);
    }

    private static View getRootView(View view) {
        View root = null;
        if (view != null){
            root = view.getRootView();
        }
        return root;
    }

    public static String viewStr(View view){
            String expectStr = "";
            if (view != null){
                if (view instanceof TextView){
                    expectStr = ((TextView)view).getText().toString();
                } else {
                    expectStr = view.toString();
                }
            }

        return expectStr;
    }
}
