// Runtime.cpp - Runtime implementation of object
//

// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Runtime functions
//////////////////////////////////////////////////////////////////////////////////
#ifdef RUN_ONLY

// ExtObject constructor:
// Only use for class initializer list.  Object initialisation must be done in OnCreate.
ExtObject::ExtObject(initialObject* editObject, VRuntime* pVRuntime)
{
	pRuntime = pVRuntime;
	info.editObject = editObject;
}

// This is called just after the constructor.  You should perform all loading here.
void ExtObject::OnCreate()
{
	bin ar;
	ar.attach(info.editObject->eData, info.editObject->eSize);

	ar.detach();

	info.w = info.editObject->eWidth;
	info.h = info.editObject->eHeight;
	
	info.x = info.editObject->eX;
	info.y = info.editObject->eY;

	returned = false;

	// Update bounding box
	pRuntime->UpdateBoundingBox(this);
}

// Destructor: called when an instance of your object is destroyed.
ExtObject::~ExtObject()
{
}

// Called every frame for you to update your object if necessary
// Return values: 1 (do not call RTOnFrame again), 0 (continue calling)
// The frame call order goes like this:
// run events -> OnFrame -> Draw -> OnFrame2 -> run events -> ...
BOOL ExtObject::OnFrame()
{
	return 1;
}

BOOL ExtObject::OnFrame2()
{
	// Check for any delayed calls that have timed out and need calling
	if (delayedCalls.empty())
		return 0;

	list<DelayedFunctionCall>::iterator i = delayedCalls.begin();

	clock_t theTime = pRuntime->GetLayoutClock(pLayout);

	for ( ; i != delayedCalls.end(); ) {
		// Time to call?
		if (i->scheduledTime <= theTime) {

			// Call
			funcStack.push_back(FunctionStack());
			FunctionStack& f = funcStack.back();

			f.name = i->funcName;
			f.paramList = i->paramList;

			// Run the events
			pRuntime->GenerateEvent(info.oid, 1, this);		// On Any Function
			pRuntime->GenerateEvent(info.oid, 0, this);		// On Function

			// Pop the func stack
			funcStack.pop_back();

			// Remove call entry
			i = delayedCalls.erase(i);
		}
		else
			i++;
	}

	return 0;
}

// Draw: Construct calls this when it wants you to draw your object.  You can
// leave it blank if your extension doesn't draw anything.
void ExtObject::Draw()
{
}

// WindowProc:  called when a window message, or WM_COMMAND specifying your window,
// is sent to Construct.  You must call RegisterWindow() before Construct will send
// you messages for the given window.
LRESULT ExtObject::WindowProc(HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam)
{
	return DefWindowProc(hWnd, uMessage, wParam, lParam);
}

BOOL ExtObject::PreTranslateMessage(MSG* msg)
{
	return 0;
}

// For global objects
void ExtObject::OnFrameChange(int oldFrame, int newFrame)
{
	// Do anything your global object needs when the frame changes
	// oldFrame is -1 on start of app, newFrame is -1 on end of app
}

void ExtObject::Serialize(bin& ar)
{
	int Version = 1;
	SerializeVersion(ar, Version);

	if (ar.loading) {
		ar >> curParams;

		int count;
		ar >> count;
		delayedCalls.resize(0);

		for (int i = 0; i < count; i++) {
			DelayedFunctionCall dfc;
			ar >> dfc.scheduledTime >> dfc.funcName >> dfc.paramList;
			delayedCalls.push_back(dfc);
		}

	}
	else {
		ar << curParams;

		ar << delayedCalls.size();

		list<DelayedFunctionCall>::iterator i = delayedCalls.begin();

		for ( ; i != delayedCalls.end(); i++)
			ar << i->scheduledTime << i->funcName << i->paramList;
	}
}

long ExtObject::GetData(int id, void* param)
{
	switch (id) {
	// If your extension uses a window, you can return it here
	case GET_WINDOW:
		break;
	}

	return 0;
}

long ExtObject::CallFunction(int id, void* param)
{
	return 0;
}

// Return the Default Value.  This is the value to be returned if your object name
// is used without an expression name, eg. in "Counter + 1".
// Parameters can be passed, eg. MyArray(3,7).
long ExtObject::ReturnDefaultValue(LPVAL theParams, ExpReturn& ret)
{
	if (funcStack.empty())
		return ret = 0;

	int index = theParams[0].GetInt() - 1;
	vector<ExpStore>& paramList = funcStack.back().paramList;

	if (index < 0 || index >= paramList.size()) {
		pRuntime->AddDebugLogMessage("Function object: Attempted to retrieve a non-existant parameter; returning 0");  
		return 0;
	}
	else return ret.ReturnCustom(pRuntime, paramList[index]);
}

// Called for undefined expression names, if your extension allows it.
// Eg: MyObject.UndefinedExpression(3,4,5) calls this routine with
// "UndefinedExpression" as the expression name.
long ExtObject::ReturnUndefinedExpression(CString& expName, LPVAL theParams, ExpReturn& ret)
{
	// Call a function of the given name
	funcStack.push_back(FunctionStack());
	FunctionStack& f = funcStack.back();

	f.name = expName;
	f.paramList.reserve(GetNumParams(theParams));
	
	LPVAL ptr = theParams;
	for ( ; ptr->Type() != EXPTYPE_NULL; ptr++)
		f.paramList.push_back(*ptr);

	returned = false;

	pRuntime->NewSOL();

	// Run the events
	pRuntime->GenerateEvent(info.oid, 1, this);		// On Any Function
	pRuntime->GenerateEvent(info.oid, 0, this);		// On Function

	pRuntime->RestoreSOL();

	if (!returned) {

		ret = 0;
	}
	else
	{
		ret.ReturnCustom(pRuntime, retVal);


	}
	// Pop the func stack
	funcStack.pop_back();

	return 0;
}

#else //ifdef RUN_ONLY

CRunObject* WINAPI RTCreateObject(bin& ar, VRuntime* pRuntime) 
{
	return NULL;
}

void WINAPI RTDestroyObject(ExtObject* object)
{
}

#endif