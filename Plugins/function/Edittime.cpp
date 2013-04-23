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
// There are several types of properties currently available to use:-
//
// PROPERTY_EDIT  , a text input field
// PROPERTY_STATIC, a static display of text
//
// Here is a brief example of them all:
//
// PROPERTY_EDIT  (myVariable, "Label", "Description);
// PROPERTY_STATIC(myVariable, "Label", "Description);

BEGIN_PROPERTY_TABLE();
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

}


// Destructor: when your object is deleted or the application closed:
// close any allocated resources here
EditExt::~EditExt()
{

}

// Draw your object in the frame editor.
// Note the co-ordinates are automatically adjusted; draw at pInfo->objectX/Y
void EditExt::Draw()
{
	// Simply blit the object icon texture to the frame
	pEditTime->SetTexture(iTexture);

	pEditTime->Blitrc(pInfo->objectX, pInfo->objectY, pInfo->objectWidth, pInfo->objectHeight);
}

// Initialization:  when the frame editor is opened.  Create textures, fonts,
// and anything else DirectX volatile here.
void EditExt::Initialize()
{
	// Make a texture from the object's image
	USEMYRESOURCES();
	iTexture = pEditTime->AddTextureFromResource2(MAKEINTRESOURCE(OBJ_ICON));
}

void EditExt::OnPropertiesUpdate()
{
}

// When the object is inserted to the frame editor by the user.
void EditExt::OnPut()
{
	pInfo->objectWidth = 32;
	pInfo->objectHeight = 32;
}


// When the object is removed from the frame editor by the user.
void EditExt::OnRemoved()
{

}

// Get any icons needed to show this ACEs menu
int EditExt::GetMenuIcons(int ACEMenu)
{
	// Todo: return the parameter number of a bitmap carrying an imagelist
	//       to show in the menu indicated by ACEMenu

	return -1; // -1 indicates no icons will be used
}


int EditExt::OnShowACEMenu(TABLE ACEType)
{
	// You can return the resource number indicating the menu to show for the
	//       ACE type indicated in ACEType
	// This overrides the default menu

	return -1;	// Return -1 for standard menu to be loaded
}

// On object resized
BOOL EditExt::OnSizeObject()
{
	// TRUE = allow sizing to occur, FALSE = disallow
	return TRUE;
}

void EditExt::Serialize(bin& ar)
{
	int Version = 1;
	SerializeVersion(ar, Version);

	if (ar.loading) {

	}
	else {

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