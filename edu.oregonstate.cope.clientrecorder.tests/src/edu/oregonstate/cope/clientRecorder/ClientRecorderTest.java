package edu.oregonstate.cope.clientRecorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

//TODO refactor this test class. Too many hardcoded strings. Too much duplication with tested class.
public class ClientRecorderTest {

	private ClientRecorder clientRecorder;

	@Before
	public void setup() {
		clientRecorder = new ClientRecorder();
		clientRecorder.setIDE("IDEA");
	}

	/* Text Change Tests */
	@Test(expected = RuntimeException.class)
	public void testRecordTextChangeNull() throws Exception {
		clientRecorder.buildTextChangeJSON(null, 0, 0, null, null);
	}

	@Test(expected = RuntimeException.class)
	public void testRecordTextChangeNoSourceFile() throws Exception {
		clientRecorder.buildTextChangeJSON("", 0, 0, "", "");
	}

	@Test(expected = RuntimeException.class)
	public void testRecordTextChangeNoOrigin() throws Exception {
		clientRecorder.buildTextChangeJSON("", 0, 0, "/sampleFile", "");
	}

	@Test
	public void testRecordTextChangeNoOp() throws Exception {
		JSONObject result1 = clientRecorder.buildTextChangeJSON("", 0, 0, "/sampleFile", "changeOrigin");
		JSONObject obj = createChangeJSON("", 0, 0, "/sampleFile", "changeOrigin");
		assertJSONEquals(result1, obj);
	}

	@Test
	public void testRecordTextChangeAdd() throws Exception {
		JSONObject result1 = clientRecorder.buildTextChangeJSON("addedText", 0, 0, "/sampleFile", "changeOrigin");
		JSONObject obj = createChangeJSON("addedText", 0, 0, "/sampleFile", "changeOrigin");
		assertJSONEquals(result1, obj);
	}

	@Test
	public void testRecordTextChangeDelete() throws Exception {
		JSONObject result1 = clientRecorder.buildTextChangeJSON("", 0, 0, "/sampleFile", "changeOrigin");
		JSONObject obj = createChangeJSON("", 0, 0, "/sampleFile", "changeOrigin");
		assertJSONEquals(result1, obj);
	}

	@Test
	public void testRecordTextChangeReplace() throws Exception {
		JSONObject result1 = clientRecorder.buildTextChangeJSON("addedText", 3, 0, "/sampleFile", "changeOrigin");
		JSONObject obj = createChangeJSON("addedText", 3, 0, "/sampleFile", "changeOrigin");
		assertJSONEquals(result1, obj);
	}
	
	@Test
	public void testRecordRefresh() {
		JSONObject actual = clientRecorder.buildRefreshJSON("new file contents", "/proj/file1");
		
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.refresh + "");
		expected.put(JSONConstants.JSON_TEXT, "new file contents");
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "/proj/file1");
		addTimeStamp(expected);
		
		assertJSONEquals(expected, actual);
	}

	private JSONObject createChangeJSON(String text, int offset, int length, String sourceFile, String changeOrigin) {
		JSONObject j = new JSONObject();
		j.put(JSONConstants.JSON_EVENT_TYPE, Events.textChange.toString());
		j.put(JSONConstants.JSON_TEXT, text);
		j.put(JSONConstants.JSON_OFFSET, offset);
		j.put(JSONConstants.JSON_LENGTH, length);
		j.put(JSONConstants.JSON_ENTITY_ADDRESS, sourceFile);
		j.put(JSONConstants.JSON_CHANGE_ORIGIN, changeOrigin);
		j.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		addTimeStamp(j);
		return j;
	}

	/* Test DebugLaunch */
	@Test(expected = RuntimeException.class)
	public void testDebugLaunchNull() throws Exception {
		clientRecorder.buildIDEEventJSON(null, null);
	}

	@Test
	public void testDebugLaunch() throws Exception {
		JSONObject retObj = clientRecorder.buildIDEEventJSON(Events.debugLaunch, "/workspace/package/filename.java");
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_IDE, "IDEA");
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.debugLaunch.toString());
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "/workspace/package/filename.java");
		addTimeStamp(expected);

		assertJSONEquals(expected, retObj);
	}

	@Test
	public void testStdLaunch() throws Exception {
		JSONObject retObj = clientRecorder.buildIDEEventJSON(Events.normalLaunch, "/workspace/package/filename.java");
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_IDE, "IDEA");
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.normalLaunch.toString());
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "/workspace/package/filename.java");
		addTimeStamp(expected);

		assertJSONEquals(expected, retObj);
	}
	
	@Test
	public void testLaunchEnd() throws Exception {
		JSONObject actual = clientRecorder.buildLaunchEndEventJSON(Events.launchEnd, "123");
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.launchEnd + "");
		expected.put(JSONConstants.JSON_LAUNCH_TIMESTAMP, "123");
		addTimeStamp(expected);
		
		assertJSONEquals(expected, actual);
	}

	@Test
	public void testFileOpen() throws Exception {
		JSONObject retObj = clientRecorder.buildIDEEventJSON(Events.fileOpen, "/workspace/package/filename.java");
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_IDE, "IDEA");
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.fileOpen.toString());
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "/workspace/package/filename.java");
		addTimeStamp(expected);

		assertJSONEquals(expected, retObj);
	}

	@Test
	public void testFileClose() throws Exception {
		JSONObject retObj = clientRecorder.buildIDEEventJSON(Events.fileClose, "/workspace/package/filename.java");
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_IDE, "IDEA");
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.fileClose.toString());
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "/workspace/package/filename.java");
		addTimeStamp(expected);

		assertJSONEquals(expected, retObj);
	}

	@Test(expected = RuntimeException.class)
	public void testTestRunNull() throws Exception {
		clientRecorder.buildTestEventJSON(null, null, 0);
	}

	@Test(expected = RuntimeException.class)
	public void testTestRunEmpty() throws Exception {
		clientRecorder.buildTestEventJSON("", "", 0);
	}

	@Test
	public void testTestRun() throws Exception {
		JSONObject actual = clientRecorder.buildTestEventJSON("/workspace/package/TestFoo/testBar", "success", 2);
		JSONObject expected = new JSONObject();

		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.testRun.toString());
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "/workspace/package/TestFoo/testBar");
		expected.put(JSONConstants.JSON_TEST_RESULT, "success");
		expected.put(JSONConstants.JSON_TEST_ELAPSED_TIME, 2.0);
		addTimeStamp(expected);

		assertJSONEquals(expected, actual);
	}
	
	@Test(expected = RuntimeException.class)
	public void testSnapshotNull() throws Exception {
		clientRecorder.buildSnapshotJSON(null);
	}
	
	@Test(expected = RuntimeException.class)
	public void testSnapshotEmpty() throws Exception {
		clientRecorder.buildSnapshotJSON("");
	}
	
	@Test
	public void testSnapshot() throws Exception {
		JSONObject actual = clientRecorder.buildSnapshotJSON("/path/to/snapshot/theSnapshot");
		
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.snapshot + "");
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "/path/to/snapshot/theSnapshot");
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		addTimeStamp(expected);
		
		assertJSONEquals(expected, actual);
	}

	private void addTimeStamp(JSONObject expected) {
		expected.put(JSONConstants.JSON_TIMESTAMP, (System.currentTimeMillis() / 1000) + "");
	}

	private void assertJSONEquals(JSONObject expected, JSONObject actual) {

		assertEquals(expected.keySet(), actual.keySet());

		for (Object key : expected.keySet()) {
			if (key.equals(JSONConstants.JSON_TIMESTAMP)) {
				assertTimestampsEqual(expected.get(key), actual.get(key));
			} else {
				assertEquals(expected.get(key), actual.get(key));
			}
		}
	}

	private void assertTimestampsEqual(Object expected, Object actual) {
		int oneSecond = 3600;
		
		Long expectedTimestamp = Long.parseLong((String) expected);
		Long actualTimestamp = Long.parseLong((String) actual);
		
		assertTrue(expectedTimestamp > actualTimestamp - oneSecond);
	}
	
	@Test
	public void testFileSave() {
		JSONObject output = clientRecorder.buildIDEEventJSON(Events.fileSave, "/workspace/project/file");
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.fileSave + "");
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "/workspace/project/file");
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		addTimeStamp(expected);
		
		assertJSONEquals(expected, output);
	}
	
	@Test
	public void testRecordLaunchEvent() {
		HashMap launchAttributes = new HashMap();
		launchAttributes.put("attr1", "something");
		launchAttributes.put("attr2", "something else");
		JSONObject actual = clientRecorder.buildLaunchEventJSON(Events.normalLaunch, "123", "launch-name", "contents", "config", launchAttributes);
		
		JSONObject expected = new JSONObject();
		addTimeStamp(expected);
		expected.put(JSONConstants.JSON_EVENT_TYPE,Events.normalLaunch + "");
		expected.put(JSONConstants.JSON_LAUNCH_TIMESTAMP, "123");
		expected.put(JSONConstants.JSON_LAUNCH_NAME, "launch-name");
		expected.put(JSONConstants.JSON_LAUNCH_FILE, "contents");
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		expected.put(JSONConstants.JSON_LAUNCH_ATTRIBUTES, launchAttributes);
		expected.put(JSONConstants.JSON_LAUNCH_CONFIGURATION, "config");
		
		assertJSONEquals(expected, actual);
	}
	
	@Test
	public void testRefactoringDo() {
		Map refactoringArguments = new HashMap();
		refactoringArguments.put("arg1", "one");
		refactoringArguments.put("arg", "two");
		JSONObject actual = clientRecorder.buildRefactoringEvent(Events.refactoringLaunch, "rename", refactoringArguments);
		
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.refactoringLaunch + "");
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		expected.put(JSONConstants.JSON_REFACTORING_ID, "rename");
		expected.put(JSONConstants.JSON_REFACTORING_ARGUMENTS, refactoringArguments);
		addTimeStamp(expected);
		
		assertJSONEquals(expected, actual);
	}
	
	@Test
	public void testRefactoringUndo() {
		Map refactoringArguments = new HashMap();
		refactoringArguments.put("arg1", "one");
		refactoringArguments.put("arg", "two");
		JSONObject actual = clientRecorder.buildRefactoringEvent(Events.refactoringUndo, "rename", refactoringArguments);
		
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.refactoringUndo + "");
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		expected.put(JSONConstants.JSON_REFACTORING_ID, "rename");
		expected.put(JSONConstants.JSON_REFACTORING_ARGUMENTS, refactoringArguments);
		addTimeStamp(expected);
		
		assertJSONEquals(expected, actual);
	}
	
	@Test
	public void testCopy() {
		JSONObject actual = clientRecorder.buildCopyJSON(Events.copy, "addr", 0, 12, "bla");
		
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.copy + "");
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "addr");
		expected.put(JSONConstants.JSON_LENGTH, 12);
		expected.put(JSONConstants.JSON_OFFSET, 0);
		expected.put(JSONConstants.JSON_TEXT, "bla");
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		addTimeStamp(expected);
		
		assertJSONEquals(expected, actual);
	}
	
	@Test
	public void testResourceDelete() {
		JSONObject actual = clientRecorder.buildResourceDeleteJSON("/some/resource.txt");
		
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.resourceRemoved + "");
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "/some/resource.txt");
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		addTimeStamp(expected);
		
		assertJSONEquals(expected, actual);
	}
	
	@Test
	public void testResourceAdd() {
		JSONObject actual = clientRecorder.buildResourceAddJSON("/some/resource.txt", "abc");
		
		JSONObject expected = new JSONObject();
		expected.put(JSONConstants.JSON_EVENT_TYPE, Events.resourceAdded + "");
		expected.put(JSONConstants.JSON_ENTITY_ADDRESS, "/some/resource.txt");
		expected.put(JSONConstants.JSON_TEXT, "abc");
		expected.put(JSONConstants.JSON_IDE, clientRecorder.getIDE());
		addTimeStamp(expected);
		
		assertJSONEquals(expected, actual);
	}
}