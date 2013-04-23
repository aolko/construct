// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Conditions
//////////////////////////////////////////////////////////////////////////////////

long ExtObject::cWaitUntil(LPVAL params)
{
	//MessageBox(0, "Wait Until was called, it should not have been!", 0, 0);
	// This implementation is never gonna be called by the runtime
	return true;
}

long ExtObject::cDelay(LPVAL params)
{
	//MessageBox(0, "Delay was called, it should not have been!", 0, 0);
	// This implementation is never gonna be called by the runtime
	return true;
}

long ExtObject::cWaitUntilActivated(LPVAL params)
{
	int id = params[0].GetInt();
	map<int, list<data> >::iterator m = memory.find(id);
	if(m == memory.end())
		return false;


	curMemory = m;
	
	list<data>& list_data = m->second;
	for( list<data>::iterator d = list_data.begin(); d!= list_data.end(); d++)
	{
		//MessageBox(0, "Wait until activated was sucessful", 0, 0);
		curData = d;

		return true; //todo: foreach
	}
	return false;
}

long ExtObject::cDelayComplete(LPVAL params)
{
	int id = params[0].GetInt();
	map<int, list<data> >::iterator m = memory.find(id);
	if(m == memory.end())
		return false;

	curMemory = m;

	list<data>& list_data = m->second;
	for( list<data>::iterator d = list_data.begin(); d!= list_data.end(); d++)
	{
		curData = d;
		if(d->testTime(pRuntime->GetLayoutClock(pLayout)))
		{
			//MessageBox(0, "Delay Complete was sucessful", 0, 0);
			list_data.erase(d);
			return true;
		}
	}
	return false;
}
