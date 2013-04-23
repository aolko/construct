// Edittime.cpp - Edittime implementation of object
//

// Include StdAfx and Properties.h
#include "StdAfx.h"

#ifndef RUN_ONLY

//////////////////////////////////////////////////////////////////////////////////
// Property table
//////////////////////////////////////////////////////////////////////////////////

// The property table houses settings for your object that you wish to 
// be configurable before runtime
//
// For example, a button object would have settings relating to the text 
// that will be displayed, and what type of button it is
//
// There are several types of properties currently available to use:
//
// PROPERTY_EDIT  , a text input field
// PROPERTY_STATIC, a static display of text
//
// Here is a brief example of them all:
//
// PROPERTY_EDIT  (myVariable,   "Label", "Description); // myVariable is a string
// PROPERTY_STATIC(myVariable,   "Label", "Description); // myVariable is a string
// PROPERTY_VALUE (myVariable,   "Label", "Description); // myVariable is an int
// PROPERTY_BUTTON("ButtonText", "Label", "Description", Function()); // Function is a void with no parameters

BEGIN_PROPERTY_TABLE();
	PROPERTY_COMBO(axis, "Axis", "The axis this object can be dragged on", "Both|Horizontal|Vertical");
	PROPERTY_COMBO(mouse_button, "Mouse button", "Mouse button that can drag the object", "Left|Middle|Right");
	PROPERTY_COMBO(allowoff, "Allow out of window", "Continue dragging while mouse is out of window", "False|True");
//	PROPERTY_VALUE(x_limit, "X limit", "The maximum horizontal drag that can occur, 0 for unlimited");
//	PROPERTY_VALUE(y_limit, "Y limit", "The maximum vertical drag that can occur, 0 for unlimited");
END_PROPERTY_TABLE  ();

/////////////////////////////////////////////////////////////////////////////////
// Edittime functions
//////////////////////////////////////////////////////////////////////////////////

// Constructor: when your object is created during edittime or application loaded.
// Initialise values here.
EditExt::EditExt(VEditTime* pVEditTime, editInfo* pEInfo)
{
	// Save the edittime and einfo pointers
	pEditTime = pVEditTime;
	pInfo = pEInfo;

	axis = 0;
	mouse_button = 0;
	allowoff=false;

	x_limit = 0;
	y_limit = 0;
}

// Destructor: when your object is deleted or the application closed:
// close any allocated resources here
EditExt::~EditExt()
{
	//
}

// Draw your object in the frame editor.
// Note the co-ordinates are automatically adjusted; draw at pInfo->objectX/Y
void EditExt::Draw()
{
	//
}

// Initialization:  when the frame editor is opened.  Create textures, fonts,
// and anything else DirectX volatile here.
void EditExt::Initialize()
{
	// 
}

// When the object is inserted to the frame editor by the user.
void EditExt::OnPut()
{

}

// When the object is removed from the frame editor by the user.
void EditExt::OnRemoved()
{
}

// Properties updated
void EditExt::OnPropertiesUpdate()
{

}

// On object resized
BOOL EditExt::OnSizeObject()
{
	// TRUE = allow sizing to occur, FALSE = disallow
	return TRUE;
}

void EditExt::Serialize(bin& ar)
{
	int Version = 3;
	SerializeVersion(ar, Version);

	if (ar.loading)
	{
		ar >> mouse_button;

		if (Version >= 2)
			ar >> axis >> x_limit >> y_limit;

		if (Version >= 3)
		{
			bool serialallow=allowoff==0?false:true;
			ar >> serialallow;
		}
		
	}
	else
	{
		ar << mouse_button;
		ar << axis;	
		ar << x_limit;
		ar << y_limit;
		ar << allowoff;
	}
}

#else //ifndef RUN_ONLY

BOOL WINAPI ETCreateObject(editInfo* editObject) {return FALSE;}
BOOL WINAPI ETInitializeObject(editInfo* editObject, VEditTime* pVEditTime) {return FALSE;}
BOOL WINAPI ETDestroyObject(editInfo* editObject) {return FALSE;}
int WINAPI ETDrawObject(editInfo *editObject) {return 0;}
void ETOnPropertyChanged(editInfo* editObject) {}
long LinkPropTable() {return 0;}
void WINAPI ETSerialize(editInfo* editObject, CArchive& ar) {}
void WINAPI ETPutObject(editInfo* editObject) {}
void WINAPI ETRemoveObject(editInfo* editObject) {}
void WINAPI GetInfo(LPOINFO objectInfo) {}
int WINAPI ETGetMenuIcons(editInfo* editObject, int ACEMenu) {return 0;}
int WINAPI ETShowACEMenu(editInfo* editObject, TABLE ACEType)  {return 0;}
BOOL WINAPI ETSizeObject(editInfo* editObject)  {return FALSE;}

#endif // RUN_ONLY