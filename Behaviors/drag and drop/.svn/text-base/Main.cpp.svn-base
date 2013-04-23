// Main.cpp - Main implementation of object
//

// Include StdAfx
#include "StdAfx.h"

// If run only
#ifdef RUN_ONLY

//////////////////////////////////////////////////////////////////////////////////
// Conditions
//////////////////////////////////////////////////////////////////////////////////
long ExtObject::cTrigger(LPVAL params)
{
	return true;
}

long ExtObject::cIsDragging(LPVAL theParams)
{
	return dragging;
}

//////////////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////////////
long ExtObject::aSetActivated(LPVAL theParams)
{
	activated = theParams[0].GetBool();

	if (activated) {
		readyInstances.resize(0);
		pRuntime->CallOnFrame(this);
		pRuntime->CallOnFrame2(this);
	}

	return 0;
}
long ExtObject::aForceDragDrop(LPVAL theParams)
{
	CRunLayer* pLayer = pRuntime->GetObjectLayer(pLink);
	switch(theParams[0].GetInt())
	{
	case 0://drop
		dragging=false;
		return 0;
	case 1://drag		
		dx = pLink->info.x - pLayer->mouseX;
		dy = pLink->info.y - pLayer->mouseY;
		dragging=true;
		return 0;
	case 2://ignore
		ignore=true;
	}
	return 0;
}

//////////////////////////////////////////////////////////////////////////////////
// Expressions
//////////////////////////////////////////////////////////////////////////////////

#endif // #ifdef RUN_ONLY

//////////////////////////////////////////////////////////////////////////////////
// ACE tables
//////////////////////////////////////////////////////////////////////////////////

void DefineACES(MicroAceTime* at)
{
	// Define your ACE tables here.
	// Use ADDPARAM before each ADDACT/ADDCND/ADDEXP to add formal parameters.
	
	// Format:
	// ADDPARAM(type, title, description)
	//
	// type = the type of parameter.  See the PARAMS enum.
	// title = title of parameter, in expression editor
	// description = text to go in the text box describing this parameter.  Try to give
	//				 a helpful description.

	/////////////////////////////
	// Conditions
	ADDCND("Is dragging", "Movement", "%o Is Dragging", &ExtObject::cIsDragging, "IsDragging", 0);
	ADDCND("On drag start", "Movement", "%o On Drag Start", &ExtObject::cTrigger, "OnDragStart", CND_TRIGGERED);
	ADDCND("On drop", "Movement", "%o On Drop", &ExtObject::cTrigger, "OnDrop", CND_TRIGGERED);

	// Format:
	// ADDCND(menuID, routine, scriptname, displaytext, flags)
	//
	// menuID = ID in the menu resource, must start at 2000
	// routine = address of the function associated
	// scriptname = reserved for future script use, e.g. "Set X position" script name could be "SetX"
	// displaytext = text to display in event editor, %0 is first param, %1 second etc
	// flags: can be
	// 0: ordinary evaluated event
	// CND_TRIGGERED: ordinary event trigger


	/////////////////////////////
	// Actions

	// ADDACT(menuID, routine, scriptname, displaytext)
	// Same format as ADDCND, but actions have no flags.

	ADDPARAMCOMBO("Activation", "Choose whether the movement is activated or deactivated", "Deactivate|Activate");
	ADDACT("Set activated", "Settings", "%0 Drag & Drop movement", &ExtObject::aSetActivated, "SetActivated", 0);	
	
	ADDPARAMCOMBO("Drag", "Choose whether to force a drag or a drop", "drop|drag|ignore");
	ADDACT("Force Drag|Drop", "Movement", "Force %0", &ExtObject::aForceDragDrop, "ForceDragDrop", 0);	

}

