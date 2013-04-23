// Include StdAfx
#include "StdAfx.h"

//functions for iterating through a list of objects, and picking them one at a time
//be sure to add the SOL_MODIFIER flag in the ACETABLE:
//ADDCND("My condition", "My category", "%o My condition", &ExtObject::cMyCondition, "MyCondition", SOL_MODIFIER);
//example:
//long ExtObject::cBareMinimumLoopingCondition(LPVAL params)
//{
//	ExtObject::StartSOLLoopCondition();
//	for(yourloopingcondition)
//	{
//		StartSOLLoopIteration();
//		//Your code that will run on that iteration immediately prior to running the actions and subconditions of this condition
//		//Example:
//		//this makes it so all (or no) instances are picked, you do this to clear the picking of that object type:
//		//pRuntime->SelectAll(yourObjectType);
//		//this picks one instance(obj) of an object type(pType): SelectF(CRunObject* obj, CRunObjType* pType)
//		//pRuntime->SelectF(yourObjectInstances[i],yourObjectType);
//
//		
//		if(IfEndSOLLoopIteration())
//			break;
//	}
//	// always on looping SOL conditions:
//	return false;
//		
//} 
void ExtObject::StartSOLLoopCondition()
{
	
     pRuntime->GetCurrentEventPtrs(epi);
}

void ExtObject::StartSOLLoopIteration()
{
     pRuntime->NewSOLCopy();
}

bool ExtObject::EndSOLLoopIteration()
{
     if (epi.pCnds && !pRuntime->RunConditions(epi.pCnds)) {
          pRuntime->RestoreSOL();
          return true;
     }


     if (epi.pActs)
          pRuntime->RunActions(epi.pActs);

     if (epi.pSubEvents)
          pRuntime->RunSubEvents(epi.pSubEvents);

     pRuntime->RestoreSOL();
     return false;
}