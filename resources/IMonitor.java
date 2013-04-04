package org.integratedmodelling.thinklab.api.listeners;


/**
 * Glorified listener with control capabilities. A monitor is passed to observe(). If it also implements
 * any other listener interfaces, these will also be honored appropriately.
 * 
 * @author Ferd
 *
 */
public interface IMonitor {

	/**
	 * Pass a string. Will also take an exception, but usually exceptions shouldn't turn into warnings.
	 * These will be reported to the user unless the verbosity is set lowest.
	 * 
	 * @param o
	 */
	public void warn(Object o);	
	
	/**
	 * Pass a string. Will also take an exception, but usually exceptions shouldn't turn into warnings.
	 * These will be reported to the user unless the verbosity is set low. Do not abuse of these -
	 * there should be only few, really necessary info messages so that things do not get lost. The
	 * class parameter is used by the client to categorize messages so they can be shown in special
	 * ways and easily identified in a list of info messages. You can leave it null or devise your own 
	 * class.
	 * 
	 * @param o
	 */
	public void info(Object info, String infoClass);
	
	/**
	 * Pass a string or an exception (usually the latter as a reaction to an exception in the execution). 
	 * These will interrupt execution from outside, so you should return after raising one of these.
	 * 
	 * @param o
	 */
	public void error(Object o);
	
	/**
	 * Any message that is just for you or is too verbose to be an info message should be sent as
	 * debug, which is not shown by default unless you enable a higher verbosity. Don't abuse of 
	 * these, either - it's still passing stuff around through REST so it's not cheap to show a 
	 * hundred messages.
	 *  
	 * @param o
	 */
	public void debug(Object o);
	
	/**
	 * This is called from the OUTSIDE as a reaction to a user action. You should not call this, but
	 * simply raise an error and return if you encounter an error condition.
	 * 
	 * @param o
	 */
	public void stop(Object o);
	
	/**
	 * When this returns true, the user has asked to interrupt the process. You should call it 
	 * regularly at strategic points in your program and react appropriately if it ever returns
	 * true. The only processes that can dispense with checking it are those that are guaranteed
	 * to run to completion quickly.
	 * 
	 * @return
	 */
	boolean isStopped();
	
	/**
	 * Call this once before the processing starts to define how many steps your process will
	 * take. You are required to report a total of n steps using addProgress() after you do this
	 * unless you raise an error. 
	 * 
	 * @param n
	 */
	public void defineProgressSteps(int n);
	
	/**
	 * Add the given amount of steps to the progress monitor. The total reported in normal
	 * processing should add up to the parameter passed to defineProgressSteps and never remain
	 * below it unless errors are thrown. Descriptions are optional (pass a null) but they can
	 * be used to document "milestones" in execution, they just work like info messages with a
	 * special class.
	 * 
	 * @param steps
	 * @param description
	 */
	public void addProgress(int steps, String description);
	
	
	
	/*
	 * change status based on control message. This is only for CLIENT use.
	 */
	public void send(String message);
	
	/*
	 * pass INotification.DEBUG, INotification.INFO or INotification.ERROR. Only for CLIENT use.
	 */
	public void setLogLevel(int level);
	
	/*
	 * return one of INotification.DEBUG, INotification.INFO or INotification.ERROR, taken from
	 * the appropriate preferences and with sensible defaults. Only for CLIENT use.
	 */
	public int getDefaultLogLevel();
	
	/*
	 * true if at least one error has been reported
	 */
	public boolean hasErrors();
}
