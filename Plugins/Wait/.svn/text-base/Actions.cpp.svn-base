// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////////////
long ExtObject::aWaitDelay(LPVAL params)
{
	//MessageBox(0,"aWaitDelay called - bad code bad!",0,0);
	// This implementation is never gonna be called by the runtime
	return 0;
}


long ExtObject::aActiveWaitDelay(LPVAL params)
{
	//MessageBox(0,"Activate wait delay",0,0);
	// Get the example parameter
	int id = params[0].GetInt();
	float delay = pRuntime->GetLayoutClock(pLayout) + params[1].GetFloat();

	memory[id].push_back(data(delay));

	map<CRunObjType*, set<CRunObject*> >& sol = memory[id].back().sol;

/*	ObjTypeList::iterator t = pLayout->typeReferences.begin();
	for(; t!= pLayout->typeReferences.end(); t++)
	{
		t->curSol->sol.clear();
	}*/


	return 0;
}

long ExtObject::aActivateWaitUntil(LPVAL params)
{
	//MessageBox(0,"Activate wait until",0,0);
	// Get the example parameter
	int id = params[0].GetInt();

	memory[id].push_back(data(0));

	// Do nothing
	return 0;
}

long ExtObject::aDeactivateWaitUntil(LPVAL params)
{
	/*
	// Okay some thinking time!

	Lets say we have
	Condition1
	Wait until
	Condition2
	Wait until
	Condition3
	      Actions


	This actually is really complicated. Basically a wait until will wait until all
	conditions below it are true

	  So the above should get exported as:
	  Condition1
		 Activate Wait(1)
	  Wait(1)
	  Condition2
		 Deactivate Wait(1)
		 Activate Wait(2)
	  Wait(2)
	  Condition3
		Deactivate Wait(2)
		Activate Wait(3)
		Actions

	 So as you can see theres never a clash between a 'Wait x' and a 'Deactivate Wait x'

	 Therefore we dont even need the id parameter...but its good to check for bugs with!
	 
	 So the next problem is that the 'wait until' can stack..for example:

	 On key space pressed
		Spawn bullet at mousex, mousey
		  Wait until
		  On key enter pressed
			   Fire bullets at mousex, mousey


	 This would mean if we press space 10 times, there would be 10 bullets and ten 'wait untils' in the stack with the same ID.
	 We need to store the index in the stack that 'wait' uses in its call to ensure the 'deactivate' matches. therefore, pointer time!
	*/
	// Deactive
	// Get the example parameter
	int param = params[0].GetInt();

	curMemory->second.erase(curData);

	//MessageBox(0,"Deactivate wait until",0,0);
	// Do nothing
	return 0;
}
