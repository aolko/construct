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
: renderer(pVRuntime->pRenderer)
{
	pRuntime = pVRuntime;
	info.editObject = editObject;
	drawInit = false;
}

// This is called just after the constructor.  You should perform all loading here.
void ExtObject::OnCreate()
{
	// Load the edittime data that was serialized.
	bin ar;
	ar.attach(info.editObject->eData, info.editObject->eSize);

	int Version = 0;
	ar >> Version;

	ar >> depth >> z >> info.angle >> scale;

	ar >> yaw >> pitch >> roll;

	yaw = RADIANS(yaw);
	pitch = RADIANS(pitch);
	roll = RADIANS(roll);

	ar >> filepath;

	needToMakeBuffers = true;


	ImageHandleInfo* imgTexture;
	imgTexture = pRuntime->LoadSerializedImageHandle(ar);
	info.curTexture = renderer->CreateTextureFromHandle(imgTexture);

	// Finished reading data
	ar.detach();

	// Set default dimensions
	info.x = info.editObject->eX;
	info.y = info.editObject->eY;
	info.w = info.editObject->eWidth;
	info.h = info.editObject->eHeight;
	//info.x += info.w / 2;
	//info.y += info.h / 2;
	
	info.HotSpotX = info.w / 2;
	info.HotSpotY = info.h / 2;
	info.HotSpotAngle = atan2(info.HotSpotY, info.HotSpotX);
	info.HotSpotDist = sqrt(info.HotSpotY*info.HotSpotY + info.HotSpotX*info.HotSpotX);

	info.collMode = COLLISIONMODE_FINE;

	// Point the custom display bounding box
	info.pDisplayBoxOverride = &displayBox;
	displayBox.left = 0;
	displayBox.top = 0;
	displayBox.right = pLayout->width;
	displayBox.bottom = pLayout->height;

	oldDetails.SetFrom(this);

	// Setup private vars
	SetupPrivateVariableVector(pRuntime, this, privateVars);

	// Update bounding box
	pRuntime->UpdateBoundingBox(this);

	myobject.load_from_file(filepath.GetBuffer());
}

// Destructor: called when an instance of your object is destroyed.
ExtObject::~ExtObject()
{
}

// Called every frame, after the events and before drawing, for you to update your object if necessary
// Return 1 (do not call again) or 0 (continue calling)
BOOL ExtObject::OnFrame()
{
	return 1;	// Do not call again.
}


void ObjectDetails::SetFrom(ExtObject* obj) {
	x = obj->info.x;
	y = obj->info.y;
	w = obj->info.w;
	h = obj->info.h;
	angle = obj->info.angle;
	scrollX = obj->pLayout->scrollX;
	scrollY = obj->pLayout->scrollY;
	yaw = obj->yaw;
	pitch = obj->pitch;
	roll = obj->roll;
}

// Called every frame, before the events and after drawing, for you to update your object if necessary
// Return 1 (do not call again) or 0 (continue calling)
BOOL ExtObject::OnFrame2()
{
	float scrollX = pLayout->scrollX;
	float scrollY = pLayout->scrollY;

	Clamp(scrollX, 0.0f, (float)pLayout->width - (float)pLayout->winWidth);
	Clamp(scrollY, 0.0f, (float)pLayout->height - (float)pLayout->winHeight);

	// Update the display box only when any of the object properties changes
	ObjectDetails currentDetails;
	currentDetails.SetFrom(this);
	currentDetails.scrollX = scrollX;
	currentDetails.scrollY = scrollY;

	if (currentDetails != oldDetails) {

		oldDetails = currentDetails;

		cr::point3d unprojected[8];

		// Calculate the screen position of each of the cube's eight vertices


		vector<cr::point3d> verts;
		for( int i = 0; i < 8; i++)
			verts.push_back( myobject.bb[i] );

		transform_vertices(verts, true);

		renderer->Project(&verts[0], unprojected, 8);


		float maxX = unprojected[0].x;
		float minX = unprojected[0].x;
		float maxY = unprojected[0].y;
		float minY = unprojected[0].y;

		for (int i = 1; i < 8; i++) {
			if (unprojected[i].x > maxX)
				maxX = unprojected[i].x;
			if (unprojected[i].x < minX)
				minX = unprojected[i].x;
			if (unprojected[i].y > maxY)
				maxY = unprojected[i].y;
			if (unprojected[i].y < minY)
				minY = unprojected[i].y;
		}

		// Form bounding box
		displayBox.left = minX + scrollX;
		displayBox.top = minY + scrollY;
		displayBox.right = maxX + scrollX;
		displayBox.bottom = maxY + scrollY;
	}

	return 0;	// Call again
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

// User defined functions
long ExtObject::GetData(int id, void* param)
{
	return 0;
}

long ExtObject::CallFunction(int id, void* param)
{
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