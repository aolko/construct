// Runtime.cpp - Runtime implementation of object
//

// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Runtime functions
//////////////////////////////////////////////////////////////////////////////////
#ifdef RUN_ONLY

struct ShadowInfo {
	CRunObject* obj;
	CRunObject* shadow;
	float* pShadowDepth;
	colshape shape;
	int vertexCount;
	point* vertex;
};

static vector<ShadowInfo> shadowList;

// ExtObject constructor:
// Called when Construct creates an instance of your object.  Use your editTime data
// to initialise the object.
ExtObject::ExtObject(initialObject* editObject, VRuntime* pVRuntime)
{
	pRuntime = pVRuntime;
	info.editObject = editObject;
}

// This is called just after the constructor, when Construct has added details to your
// object such as siblings and type pointer.  If you need this data on startup, it is
// valid to read here.
void ExtObject::OnCreate()
{
	activated = true;

	bin ar;
	ar.attach(info.editObject->eData, info.editObject->eSize);

	int Version;
	ar >> Version >> shadowDepth;
	shape = shape_rectangle;
	int count = 0;
	if(Version >= 2)
	{
		ar >> shape;
		ar >> count;
		for (int i = 0; i < count; i++)
		{
			point pt;
			ar >> pt.x;
			ar >> pt.y;
			vertices.push_back(pt);
		}
	}

	ar.detach();

	if (!pRuntime->GlobalKeyExists("ShadowList"))
		pRuntime->SetGlobalKey("ShadowList", &shadowList);

	// Add me to shadowList
	ShadowInfo si;
	si.obj = pLink;
	si.shadow = this;
	si.pShadowDepth = &shadowDepth;
	si.shape = shape;
	si.vertexCount = count;
	si.vertex = NULL;
	if(vertices.size())
		si.vertex = &vertices[0];

	shadowList.push_back(si);
	activated=true;
}

// Destructor: called when an instance of your object is destroyed.
ExtObject::~ExtObject()
{
	// Delete me from shadowList
	vector<ShadowInfo>::iterator i = shadowList.begin();
	vector<ShadowInfo>::iterator end = shadowList.end();

	for ( ; i != end; i++) {
		if (i->shadow == this) {
			shadowList.erase(i);
			break;
		}
	}
}

// Called every frame for you to update your object if necessary
// Return values: 1 (do not call RTOnFrame again), 0 (continue calling)

// Change actual co-ordinates and cause rotation in OnFrame since these must both be
// overridable in the following events
BOOL ExtObject::OnFrame()
{
	return 1;
}

BOOL ExtObject::OnFrame2()
{
	return 1;
}

// Draw: Construct calls this when it wants you to draw your object.  You can
// leave it blank if your extension doesn't draw anything.
void ExtObject::Draw()
{
	//
}

// WindowProc:  called when a window message, or WM_COMMAND specifying your window,
// is sent to Construct.  You must call RegisterWindow() before Construct will send
// you messages for the given window.
LRESULT ExtObject::WindowProc(HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam)
{
	return -2;
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
	if (ar.loading) {
		ar >> activated >> shadowDepth;
	}
	else {
		ar << activated << shadowDepth;
	}
}

long ExtObject::GetData(int id, void* param)
{
	switch (id) {
	// If your extension uses a window, you can return it here
	case GET_WINDOW:
		break;
	case 1:
		return (long)activated;
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
	return ret = 0;
}

// Called for undefined expression names, if your extension allows it.
// Eg: MyObject.UndefinedExpression(3,4,5) calls this routine with
// "UndefinedExpression" as the expression name.
long ExtObject::ReturnUndefinedExpression(CString& expName, LPVAL theParams, ExpReturn& ret)
{
	return ret = 0;
}

#else //ifdef RUN_ONLY

CRunObject* WINAPI RTCreateObject(bin& ar, VRuntime* pRuntime) 
{
	return NULL;
}

void WINAPI RTDestroyObject(ExtObject* object)
{
	//
}

#endif