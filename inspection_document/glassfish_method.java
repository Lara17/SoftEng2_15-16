/**
 * Distributes commit messages to all Resources in the registered state
 * <p>
 * (i.e. not including those that voted VoteReadOnly). All Resources that
 * return successfully have their state set to completed; those that
 * raise heuristic exceptions are added to the
 * RegisteredResourcesHeuristic section of the CoordinatorLog and have
 * their state set to heuristic.
 * <p>
 * All Resources in the heuristic state are then told to forget,
 * their state is set to completed, and the CoordinatorLog object
 * is forced to the physical log.
 *
 * @param
 *
 * @return
 *
 * @exception HeuristicMixed  Indicates that heuristic decisions have been
 *   taken which have resulted in part of the transaction
 *   being rolled back.
 * @exception HeuristicHazard  Indicates that heuristic decisions may have
 *   been taken which have resulted in part of the transaction
 *   being rolled back.
 *
 * @see
 */
void distributeCommit() throws HeuristicMixed, HeuristicHazard, NotPrepared {
	boolean infiniteRetry = true;

	boolean heuristicException = false;
	boolean heuristicMixed = false;
	int heuristicRollback = 0;
	int heuristicCommit = 0;
	int success = 0;

	// First, get the retry count.

	/**
	if (commitRetries == -1 && commitRetryVar != null) {
		try {
			commitRetries = Integer.parseInt(commitRetryVar);
		} catch (Throwable e) {}

		infiniteRetry = false;
	}
	**/
	int commitRetries = Configuration.getRetries();
	if (commitRetries >= 0)
		infiniteRetry = false;


	// Browse through the participants, committing them. The following is
	// intended to be done asynchronously as a group of operations.

	boolean transactionCompleted = true;
	String msg = null;
	for (int i = 0; i < nRes; i++) {
		boolean isProxy = false;
		Resource currResource = (Resource) resourceObjects.get(i);

		// If the current Resource in the browse is not in the registered
		// state, skip over it.

		if ((ResourceStatus) resourceStates.get(i) ==
				ResourceStatus.Registered) {

			boolean heuristicRaised = false;

			// We determine here whether the object is a proxy because the
			// object may not exist when the commit returns.

			// COMMENT(Ram J) the instanceof operation should be replaced
			// by a is_local() call, once the local object contract is
			// implemented.
			if(!(currResource instanceof com.sun.jts.jtsxa.OTSResourceImpl)) {
				ProxyChecker checkProxy = Configuration.getProxyChecker();
				isProxy = checkProxy.isProxy(currResource);
			}

			// Change the current Resource's state to completing.

			resourceStates.set(i,ResourceStatus.Completing);

			// Tell the resource to commit.
			// Catch any exceptions here; keep going until
			// no exception is left.

			int commitRetriesLeft = commitRetries;
			boolean exceptionThrown = true;
			while (exceptionThrown) {
				try {
					if(_logger.isLoggable(Level.FINER))
					{
						_logger.logp(Level.FINER,"RegisteredResources",
								"distributeCommit()",
								"Before invoking commit on resource = " +
								currResource);
					}
					currResource.commit();
					if(_logger.isLoggable(Level.FINER))
					{
						_logger.logp(Level.FINER,"RegisteredResources",
								"distributeCommit()",
								"After invoking commit on resource = "+
								currResource);
					}
					exceptionThrown = false;
				} catch (Throwable exc) {

					if (exc instanceof HeuristicCommit || 
						// Work around the fact that org.omg.CosTransactions.ResourceOperations#commit
						// does not declare HeuristicCommit exception
						(exc instanceof HeuristicHazard && exc.getCause() instanceof XAException && 
							((XAException)exc.getCause()).errorCode == XAException.XA_HEURCOM)) {

						// If the exception is Heuristic Commit, remember
						// that a heuristic exception has been raised.
						heuristicException = true;
						heuristicRaised = true;
						heuristicMixed = true;
						exceptionThrown = false;
						heuristicCommit++;

					} else if (exc instanceof HeuristicRollback ||
							   exc instanceof HeuristicHazard ||
							   exc instanceof HeuristicMixed) {
						// If the exception is Heuristic Rollback,
						// Mixed or Hazard, remember that a heuristic
						// exception has been raised, and also that
						// damage has occurred.

						heuristicException = true;
						if (exc instanceof HeuristicRollback) {
							heuristicRollback++;
						}
						heuristicMixed = !(exc instanceof HeuristicHazard);
						heuristicRaised = true;
						exceptionThrown = false;

					} else if (exc instanceof INV_OBJREF ||
							   exc instanceof OBJECT_NOT_EXIST) {

						// If the exception is INV_OBJREF, then the target
						// Resource object must have already committed.
						exceptionThrown = false;

					} else if (exc instanceof NotPrepared) {

						// If the exception is NotPrepared, then the target
						// Resource has not recorded the fact that it has
						// been called for prepare, or some internal glitch
						// has happened inside the RegisteredResources /
						// TopCoordinator.  In this case the only sensible
						// action is to end the process with a fatal error
						// message.
						_logger.log(Level.SEVERE,
								"jts.exception_on_resource_operation",
								 new java.lang.Object[] {exc.toString(),"commit"});
						
						 throw (NotPrepared)exc;
						 /**
						 msg = LogFormatter.getLocalizedMessage(_logger,
											"jts.exception_on_resource_operation",
											new java.lang.Object[] {exc.toString(),
											"commit"});
						 throw  new org.omg.CORBA.INTERNAL(msg);
						 **/
					} else if (!(exc instanceof TRANSIENT) &&
							   !(exc instanceof COMM_FAILURE)) {
						// If the exception is neither TRANSIENT or
						// COMM_FAILURE, it is unexpected, so display a
						// message and give up with this Resource.

						//$ CHECK WITH DSOM FOLKS FOR OTHER EXCEPTIONS
						_logger.log(Level.SEVERE,
								"jts.exception_on_resource_operation",
								 new java.lang.Object[] {exc.toString(),"commit"});
						
						 exceptionThrown = false;
						 transactionCompleted = false;
						 msg = LogFormatter.getLocalizedMessage(_logger,
											"jts.exception_on_resource_operation",
											new java.lang.Object[] {exc.toString(),
											"commit"});

					} else if (commitRetriesLeft > 0 || infiniteRetry) {

						// For TRANSIENT or COMM_FAILURE, wait
						// for a while, then retry the commit.
						if (!infiniteRetry) {
							commitRetriesLeft--;
						}

						try {
							Thread.sleep(Configuration.COMMIT_RETRY_WAIT);
						} catch( Throwable e ) {}

					} else {

						// If the retry limit has been exceeded,
						// end the process with a fatal error.
			_logger.log(Level.SEVERE,"jts.retry_limit_exceeded",
							   new java.lang.Object[] {commitRetries, "commit"});

						 exceptionThrown = false;
						 transactionCompleted = false;
			 msg = LogFormatter.getLocalizedMessage(_logger,
				"jts.retry_limit_exceeded",
							new java.lang.Object[] {commitRetries, "commit"});
					}
				}
			}


			if (heuristicRaised) {

				// Either mark the participant as having raised a heuristic
				// exception, or as completed.

				resourceStates.set(i,ResourceStatus.Heuristic);

				if (logRecord != null) {
					if (!(currResource instanceof OTSResourceImpl)) {
						if (heuristicLogSection == null)
							heuristicLogSection = 
									logRecord.createSection(HEURISTIC_LOG_SECTION_NAME);
						logRecord.addObject(heuristicLogSection, currResource);
					}
				}
			} else {

				// If completed, and the object is a proxy,
				// release the proxy now.

				resourceStates.set(i,ResourceStatus.Completed);
				success++;
				if (isProxy) {
					currResource._release();
				}
			}
		}
	}

	// The browse is complete.
	// If a heuristic exception was raised, perform forget processing. This
	// will then throw the appropriate heuristic exception to the caller.
	// Note that HeuristicHazard exception with be converted to the HeuristicRolledbackException
	// by the caller

	if (heuristicException) {
	  boolean heuristicHazard = true;
	  if ((heuristicCommit + success) == nRes) {
		  heuristicMixed = false;
		  heuristicHazard = false;
	  } else if (heuristicRollback == nRes) {
		  heuristicMixed = false;
	  }
	  distributeForget(commitRetries, infiniteRetry, heuristicHazard, heuristicMixed);
	}

	if (!transactionCompleted) {
		if (coord != null)
			RecoveryManager.addToIncompleTx(coord, true);
		if (msg !=  null)
			throw  new org.omg.CORBA.INTERNAL(msg);
		else
			throw  new org.omg.CORBA.INTERNAL();
	}

	// Otherwise just return normally.
}