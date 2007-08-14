/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.test.stepping;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.debug.testplugin.DebugElementEventWaiter;
import org.eclipse.jdt.debug.tests.AbstractDebugTest;

import com.sun.jdi.InvalidTypeException;

/**
 * Tests forcing return from method.
 * 
 * @since 3.3
 */
public class ForceReturnTests extends AbstractDebugTest {

	/**
	 * Creates test case.
	 * 
	 * @param name test name
	 */
	public ForceReturnTests(String name) {
		super(name);
	}
	
	/**
	 * Tests forcing the return of an integer from top stack frame
	 * 
	 * @throws Exception
	 */
	public void testForceIntReturnTopFrame() throws Exception {
		String typeName = "ForceReturnTests";
		ILineBreakpoint bp2 = createLineBreakpoint(22, typeName);
		ILineBreakpoint bp = createLineBreakpoint(31, typeName);
		
		IJavaThread thread = null;
		try {
			thread= launchToLineBreakpoint(typeName, bp, false);
			IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
			IJavaDebugTarget target = (IJavaDebugTarget) stackFrame.getDebugTarget();
			if (target.supportsForceReturn()) {
				assertTrue("Force return should be enabled", stackFrame.canForceReturn());
				DebugElementEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.SUSPEND, thread);
				stackFrame.forceReturn(target.newValue(42));
				Object source = waiter.waitForEvent();
				assertTrue("Suspend should be from thread", source instanceof IJavaThread);
				thread = (IJavaThread) source;
				stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				if (stackFrame.getLineNumber() < 22) {
					// @see bug 197282. Some VMs optimize the variable assignment and may
					// already have performed the assignment
					thread = resumeToLineBreakpoint(thread, bp2);
					stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				}
				IJavaVariable var = stackFrame.findVariable("x");
				assertNotNull("Missing variable 'x'", var);
				assertEquals("Return value incorrect", target.newValue(42), var.getValue());
			}
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}		
	}
	
	/**
	 * Tests forcing the return of an integer from non-top stack frame
	 * 
	 * @throws Exception
	 */
	public void testForceIntReturn() throws Exception {
		String typeName = "ForceReturnTestsTwo";
		ILineBreakpoint bp2 = createLineBreakpoint(23, typeName);
		ILineBreakpoint bp = createLineBreakpoint(37, typeName);
		
		IJavaThread thread = null;
		try {
			thread= launchToLineBreakpoint(typeName, bp, false);
			IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
			IJavaDebugTarget target = (IJavaDebugTarget) stackFrame.getDebugTarget();
			if (target.supportsForceReturn()) {
				assertTrue("Force return should be enabled", stackFrame.canForceReturn());
				DebugElementEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.SUSPEND, thread);
				IJavaValue retValue = target.newValue(1);
				stackFrame = (IJavaStackFrame) thread.getStackFrames()[1];
				stackFrame.forceReturn(retValue);
				Object source = waiter.waitForEvent();
				assertTrue("Suspend should be from thread", source instanceof IJavaThread);
				thread = (IJavaThread) source;
				stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				if (stackFrame.getLineNumber() < 23) {
					// @see bug 197282. Some VMs optimize the variable assignment and may
					// already have performed the assignment
					thread = resumeToLineBreakpoint(thread, bp2);
					stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				}
				IJavaVariable var = stackFrame.findVariable("x");
				assertNotNull("Missing variable 'x'", var);
				assertEquals("Return value incorrect", retValue, var.getValue());
			}
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}		
	}	
	
	/**
	 * Tests forcing the return of a string from top frame
	 * 
	 * @throws Exception
	 */
	public void testForceStringReturnTopFrame() throws Exception {
		String typeName = "ForceReturnTests";
		ILineBreakpoint bp2 = createLineBreakpoint(24, typeName);
		ILineBreakpoint bp = createLineBreakpoint(36, typeName);
		
		IJavaThread thread = null;
		try {
			thread= launchToLineBreakpoint(typeName, bp, false);
			IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
			IJavaDebugTarget target = (IJavaDebugTarget) stackFrame.getDebugTarget();
			if (target.supportsForceReturn()) {
				assertTrue("Force return should be enabled", stackFrame.canForceReturn());
				DebugElementEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.SUSPEND, thread);
				IJavaValue stringValue = target.newValue("forty two");
				stackFrame.forceReturn(stringValue);
				Object source = waiter.waitForEvent();
				assertTrue("Suspend should be from thread", source instanceof IJavaThread);
				thread = (IJavaThread) source;
				stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				if (stackFrame.getLineNumber() < 24) {
					// @see bug 197282. Some VMs optimize the variable assignment and may
					// already have performed the assignment
					thread = resumeToLineBreakpoint(thread, bp2);
					stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				}
				IJavaVariable var = stackFrame.findVariable("s");
				assertNotNull("Missing variable 's'", var);
				assertEquals("Return value incorrect", stringValue, var.getValue());
			}
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}		
	}	
	
	/**
	 * Tests forcing the return of a string from a non top frame
	 * 
	 * @throws Exception
	 */
	public void testForceStringReturn() throws Exception {
		String typeName = "ForceReturnTestsTwo";
		ILineBreakpoint bp2 = createLineBreakpoint(25, typeName);
		ILineBreakpoint bp = createLineBreakpoint(46, typeName);
		
		IJavaThread thread = null;
		try {
			thread= launchToLineBreakpoint(typeName, bp, false);
			IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
			IJavaDebugTarget target = (IJavaDebugTarget) stackFrame.getDebugTarget();
			if (target.supportsForceReturn()) {
				assertTrue("Force return should be enabled", stackFrame.canForceReturn());
				DebugElementEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.SUSPEND, thread);
				IJavaValue stringValue = target.newValue("forty two");
				stackFrame = (IJavaStackFrame) thread.getStackFrames()[1];
				stackFrame.forceReturn(stringValue);
				Object source = waiter.waitForEvent();
				assertTrue("Suspend should be from thread", source instanceof IJavaThread);
				thread = (IJavaThread) source;
				stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				if (stackFrame.getLineNumber() < 25) {
					// @see bug 197282. Some VMs optimize the variable assignment and may
					// already have performed the assignment
					thread = resumeToLineBreakpoint(thread, bp2);
					stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				}
				IJavaVariable var = stackFrame.findVariable("s");
				assertNotNull("Missing variable 's'", var);
				assertEquals("Return value incorrect", stringValue, var.getValue());
			}
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}		
	}		

	/**
	 * Tests forcing the return of an object from top frame.
	 * 
	 * @throws Exception
	 */
	public void testForceObjectReturnTopFrame() throws Exception {
		String typeName = "ForceReturnTests";
		ILineBreakpoint bp2 = createLineBreakpoint(26, typeName);
		ILineBreakpoint bp = createLineBreakpoint(43, typeName);
		
		IJavaThread thread = null;
		try {
			thread= launchToLineBreakpoint(typeName, bp, false);
			IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
			IJavaDebugTarget target = (IJavaDebugTarget) stackFrame.getDebugTarget();
			if (target.supportsForceReturn()) {
				assertTrue("Force return should be enabled", stackFrame.canForceReturn());
				DebugElementEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.SUSPEND, thread);
				IJavaValue objectValue = target.newValue("a string");
				stackFrame.forceReturn(objectValue);
				Object source = waiter.waitForEvent();
				assertTrue("Suspend should be from thread", source instanceof IJavaThread);
				thread = (IJavaThread) source;
				stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				if (stackFrame.getLineNumber() < 26) {
					// @see bug 197282. Some VMs optimize the variable assignment and may
					// already have performed the assignment
					thread = resumeToLineBreakpoint(thread, bp2);
					stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				}
				IJavaVariable var = stackFrame.findVariable("v");
				assertNotNull("Missing variable 'v'", var);
				assertEquals("Return value incorrect", objectValue, var.getValue());
			}
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}		
	}
	
	/**
	 * Tests forcing the return of an object from non-top frame.
	 * 
	 * @throws Exception
	 */
	public void testForceObjectReturn() throws Exception {
		String typeName = "ForceReturnTestsTwo";
		ILineBreakpoint bp2 = createLineBreakpoint(27, typeName);
		ILineBreakpoint bp = createLineBreakpoint(56, typeName);
		
		IJavaThread thread = null;
		try {
			thread= launchToLineBreakpoint(typeName, bp, false);
			IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
			IJavaDebugTarget target = (IJavaDebugTarget) stackFrame.getDebugTarget();
			if (target.supportsForceReturn()) {
				assertTrue("Force return should be enabled", stackFrame.canForceReturn());
				DebugElementEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.SUSPEND, thread);
				IJavaValue objectValue = target.newValue("a string");
				stackFrame = (IJavaStackFrame) thread.getStackFrames()[1];
				stackFrame.forceReturn(objectValue);
				Object source = waiter.waitForEvent();
				assertTrue("Suspend should be from thread", source instanceof IJavaThread);
				thread = (IJavaThread) source;
				stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				if (stackFrame.getLineNumber() < 27) {
					// @see bug 197282. Some VMs optimize the variable assignment and may
					// already have performed the assignment
					thread = resumeToLineBreakpoint(thread, bp2);
					stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
				}
				IJavaVariable var = stackFrame.findVariable("v");
				assertNotNull("Missing variable 'v'", var);
				assertEquals("Return value incorrect", objectValue, var.getValue());
			}
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}		
	}	
	
	/**
	 * Tests that an incompatible type causes an exception in top frame
	 * 
	 * @throws Exception
	 */
	public void testIncompatibleReturnTypeTopFrame() throws Exception {
		String typeName = "ForceReturnTests";
		ILineBreakpoint bp = createLineBreakpoint(43, typeName);
		
		IJavaThread thread = null;
		try {
			thread= launchToLineBreakpoint(typeName, bp, false);
			IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
			IJavaDebugTarget target = (IJavaDebugTarget) stackFrame.getDebugTarget();
			if (target.supportsForceReturn()) {
				assertTrue("Force return should be enabled", stackFrame.canForceReturn());
				IJavaValue objectValue = target.newValue(42);
				try {
					stackFrame.forceReturn(objectValue);
				} catch (DebugException e) {
					assertTrue("Should be invalid type exception", e.getStatus().getException() instanceof InvalidTypeException);
					return;
				}
				assertTrue("Should have caused incompatible return type exception", false);
			}
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}		
	}	
	
	/**
	 * Tests that an incompatible type causes an exception in non top frame
	 * 
	 * @throws Exception
	 */
	public void testIncompatibleReturnType() throws Exception {
		String typeName = "ForceReturnTestsTwo";
		ILineBreakpoint bp = createLineBreakpoint(46, typeName);
		
		IJavaThread thread = null;
		try {
			thread= launchToLineBreakpoint(typeName, bp, false);
			IJavaStackFrame stackFrame = (IJavaStackFrame) thread.getTopStackFrame();
			IJavaDebugTarget target = (IJavaDebugTarget) stackFrame.getDebugTarget();
			if (target.supportsForceReturn()) {
				assertTrue("Force return should be enabled", stackFrame.canForceReturn());
				IJavaValue objectValue = target.newValue(42);
				stackFrame = (IJavaStackFrame) thread.getStackFrames()[1];
				try {
					stackFrame.forceReturn(objectValue);
				} catch (DebugException e) {
					assertTrue("Should be invalid type exception", e.getStatus().getException() instanceof InvalidTypeException);
					return;
				}
				assertTrue("Should have caused incompatible return type exception", false);
			}
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}		
	}		
}
