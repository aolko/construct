// Runtime.cpp - Runtime implementation of object
//

// Include StdAfx
#include "StdAfx.h"

//////////////////////////////////////////////////////////////////////////////////
// Runtime functions
//////////////////////////////////////////////////////////////////////////////////
#ifdef RUN_ONLY

vector<CRunObject*> ExtObject::readyInstances;

// ExtObject constructor:
// Called when Construct creates an instance of your object.  Use your editTime data
// to initialise the object.
ExtObject::ExtObject(initialObject* editObject, VRuntime* pVRuntime)
{
	pRuntime =  pVRuntime;
	info.editObject = editObject;
}

// This is called just after the constructor, when Construct has added details to your
// object such as siblings and type pointer.  If you need this data on startup, it is
// valid to read here.
void ExtObject::OnCreate()
{
	ignore=false;
	allowoff=true;
	bin ar;
	ar.attach(info.editObject->eData, info.editObject->eSize);
	
	int Version = 0;
	ar >> Version;

	ar >> mouseButton >> axis >> x_limit >> y_limit;
	if(Version>=3)
		ar >> allowoff;
	ar.detach();

	mouse[0] = false;
	mouse[1] = false;
	mouse[2] = false;

	dragging = false;
	activated = true;

	want_to_start = false;

	x_gone = 0;
	y_gone = 0;

	// Receive app window messages
	pRuntime->RegisterWindow(pRuntime->GetAppHwnd(), this);
}

// Destructor: called when an instance of your object is destroyed.
ExtObject::~ExtObject()
{
	//
}

// Called every frame for you to update your object if necessary
// Return values: 1 (do not call RTOnFrame again), 0 (continue calling)

// Change actual co-ordinates and cause rotation in OnFrame since these must both be
// overridable in the following events
BOOL ExtObject::OnFrame()
{
	
	if (want_to_start) 
	{
		// Check if others want to start and pick topmost
		if (readyInstances.size() > 1)
		{
			// Iterate these other instances and see if any of them are above me
			int myLayer = pLink->info.layer;
			int myZ = pLink->info.z;

			vector<CRunObject*>::iterator i = readyInstances.begin();

			for ( ; i != readyInstances.end(); i++)
			{
				if ((*i) == this) continue;

				int theirLayer = (*i)->pLink->info.layer;
				int theirZ = (*i)->pLink->info.z;

				// Someone is above me, dont let me drag
				if (((theirLayer > myLayer) || ((theirLayer == myLayer) && (theirZ > myZ))))
					want_to_start = false;
			}

		}

		// If still can start, start
		if (want_to_start) 
		{
			dragging = true;

			// On Drag Start
			TriggerEvent(1);
		}
	}

	want_to_start = false;

	// do dragging
		CWnd parentWnd;
	parentWnd.Attach(pRuntime->GetFrameHwnd(pLayout));


	POINT mouse;
			GetCursorPos(&mouse);
			pRuntime->ScreenToScreen(mouse);
	if (dragging&&!ignore&&(allowoff||IsMouseOverWindow(parentWnd.m_hWnd, mouse.x, mouse.y,true))) 
	{
		CRunLayer* pLayer = pRuntime->GetObjectLayer(pLink);

		// check the axis
		if (axis == 0)
		{
			// work out how far this will take us
		//	int travelled_x = pLayer->mouseX + dx - pLink->info.x;
		//	int travelled_y = pLayer->mouseY + dy - pLink->info.y;

			pLink->info.x = pLayer->mouseX + dx;
			pLink->info.y = pLayer->mouseY + dy;
		}

		else if (axis == 1)
			pLink->info.x = pLayer->mouseX + dx;

		else if (axis == 2)
			pLink->info.y = pLayer->mouseY + dy;

		pRuntime->UpdateBoundingBox(pLink);
	}
	ignore=false;
parentWnd.Detach();
	return !activated;
}

BOOL ExtObject::OnFrame2()
{
	readyInstances.resize(0);
	return !activated;
}

// Draw: Construct calls this when it wants you to draw your object.  You can
// leave it blank if your extension doesn't draw anything.
void ExtObject::Draw()
{
	//
}

void ExtObject::CheckStartDrag(int button)
{
	// If the right kind of click
	if (button == mouseButton) {

		// Get mouse position in screen coords
		POINT mouse;
		GetCursorPos(&mouse);
		ScreenToClient(pRuntime->GetFrameHwnd(pLayout), &mouse);

		CRunLayer* pLayer = pRuntime->GetObjectLayer(pLink);

		// Check for overlap on object
		if (pRuntime->QueryScreenPointInstanceCollision(mouse.x, mouse.y, pLink)&&activated) {

			// Mark ready to start, so can filter objects below in Z order
			want_to_start = true;
			readyInstances.push_back(this);

			dx = pLink->info.x - pLayer->mouseX;
			dy = pLink->info.y - pLayer->mouseY;
		}
	}
}

void ExtObject::CheckEndDrag(int button)
{
	// If the right kind of click
	if (button == mouseButton) {

		if (dragging)
			TriggerEvent(2);	// On Dropped

		dragging = false;
	}
}

// WindowProc:  called when a window message, or WM_COMMAND specifying your window,
// is sent to Construct.  You must call RegisterWindow() before Construct will send
// you messages for the given window.
LRESULT ExtObject::WindowProc(HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam)
{
	// Look for left/right/up/down keys
	switch (uMessage) {

	case WM_LBUTTONDOWN:
		mouse[0] = true;
		CheckStartDrag(0);
		break;
	case WM_LBUTTONUP:
		mouse[0] = false;
		CheckEndDrag(0);
		break;
	case WM_MBUTTONDOWN:
		mouse[1] = true;
		CheckStartDrag(1);
		break;
	case WM_MBUTTONUP:
		mouse[1] = false;
		CheckEndDrag(1);
		break;
	case WM_RBUTTONDOWN:
		mouse[2] = true;
		CheckStartDrag(2);
		break;
	case WM_RBUTTONUP:
		mouse[2] = false;
		CheckEndDrag(2);
		break;
	}
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

void ExtObject::DebuggerUpdateDisplay(ExpStore*& pPrivateVars)
{
	// No properties
}

void ExtObject::OnDebuggerValueChanged(const char* name, const char* value)
{
	// Nothing can be changed
}

void ExtObject::Serialize(bin& ar)
{
	
	if (ar.loading) {
		// Reset drag state
		dragging = false;

		ar >> mouseButton >> activated >> allowoff;

		if (activated)
			pRuntime->CallOnFrame(this);
	}
	else {
		ar << mouseButton << activated << allowoff;
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
	return ret = 0;
}

// Called for undefined expression names, if your extension allows it.
// Eg: MyObject.UndefinedExpression(3,4,5) calls this routine with
// "UndefinedExpression" as the expression name.
long ExtObject::ReturnUndefinedExpression(CString& expName, LPVAL theParams, ExpReturn& ret)
{
	return ret = 0;
}

const bool ExtObject::IsMouseOverWindow(HWND hWnd, const int mx, const int my,
                          const bool inClientSpace /*= false */)
{
    RECT windowRect;
 
    // Get the window in screen space
	::GetWindowRect( hWnd, &windowRect );
 
    if ( inClientSpace )
    {
        POINT offset;
        offset.x = offset.y = 0;
        ClientToScreen( hWnd, &offset );
 
        // Offset the window to client space
        windowRect.left -= offset.x;
        windowRect.top -= offset.y;
        // NOTE: left and top should now be 0, 0
        windowRect.right -= offset.x;
        windowRect.bottom -= offset.y;
    }
 
    // Test if mouse over window
    POINT cursorPos = { mx, my };
    return PtInRect( &windowRect, cursorPos );
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