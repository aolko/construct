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
// It is not safe to make runtime calls here: do so in OnCreate.
ExtObject::ExtObject(initialObject* editObject, VRuntime* pVRuntime)
: renderer(pVRuntime->pRenderer)
{
	pRuntime = pVRuntime;
	info.editObject = editObject;
}

// This is called just after the constructor when your object has been created.  Construct has set
// up your object here so it is safe to make runtime calls.
void ExtObject::OnCreate()
{
	int myValue;

	// Load the edittime data that was serialized.
	bin ar;
	ar.attach(info.editObject->eData, info.editObject->eSize);

	// Read the data.  Same format as you exported in EditExt::Serialize.
	// Your runtime loader must be able to load all versions!
	int Version = 0;
	ar >> Version;

	ar >> width;
	ar >> maxPoints;
	ar >> columns;

	// Finished reading data
	ar.detach();

	// Set default dimensions
	info.x = info.editObject->eX;
	info.y = info.editObject->eY;
	info.w = info.editObject->eWidth;
	info.h = info.editObject->eHeight;
	info.angle = 0.0f;

	step_offset = 0;
	step = (float)maxPoints / (float)columns; 

	// Update bounding box
	pRuntime->UpdateBoundingBox(this);
}

// Destructor: called when an instance of your object is destroyed.
ExtObject::~ExtObject()
{
}

void ExtObject::findSpriteRoutines()
{
}

void ExtObject::setSpriteDistortionAt(int col, int row, cr::point pos)
{

}

void ExtObject::createRibbonNormals() 
{
	// On frame, the plugin needs to tell the sprite object its distortion array.
	cr::point previousPosition;
	int index = 0;
	for( RibbonNodeList::iterator i = nodes.begin(); i!= nodes.end(); i++, index++)
	{
		// If its the first loop, set the previous position
		if(index == 0)
			previousPosition = i->pos;

		// As long as its not the last node, make n point to the next node
		RibbonNodeList::iterator n = i;
		if( index != nodes.size() - 1 )
			n++;
		cr::point nextPosition = n->pos;
		cr::point normal = nextPosition - previousPosition;
		normal.rotate(cr::to_radians(-90.0f));

		// Normalize (really this should be part of the point class!)
		float normalLength = normal.distance();
		if(normalLength > 0)
			normal /= normalLength;

		i->normal = normal;

		previousPosition = i->pos;	
	}
}

// Called every frame, before the events and after drawing, for you to update your object if necessary
// Return 1 (do not call again) or 0 (continue calling)
BOOL ExtObject::OnFrame()
{
	// Okay now we need to loop all our positions and tell the Sprite plugin to set the distortion points...
	/*int index = 0;
	for( RibbonNodeList::iterator i = nodes.begin(); i!= nodes.end(); i++, index++)
	{
		cr::point a = i->pos + i->normal * width;
		cr::point b = i->pos + i->normal * -width;

		setSpriteDistortionAt(index, 0, a);
		setSpriteDistortionAt(index, 1, b);
	}*/

	return 1;	// Do not call again
}

// Called every frame, after the events and before drawing, for you to update your object if necessary
// Return 1 (do not call again) or 0 (continue calling)
// It is not safe to destroy objects in OnFrame2().  If you have to do this, use OnFrame().
// If you are storing any pointers to CRunObjects, check the info.destroying flag here.  If it is true,
// you must reset the pointer to NULL as it is no longer valid.
BOOL ExtObject::OnFrame2()
{
	return 1;	// Do not call again
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