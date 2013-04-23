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
// PROPERTY_EDIT  (myVariable, "Label", "Description);
// PROPERTY_STATIC(myVariable, "Label", "Description);

/*
void WINAPI ETOnPropertyChanged(editInfo* editObject, LPCSTR szLabel, int iMode, CVirtualPropItem* item, PropReturn* Property, MicroEdittime* pm)
{
	CString SLabel = szLabel;

	if (iMode == MODE_INIT) {
		item->Type = 0;
		ASSIGN_CSTRING(item->Label, "My label");
		ASSIGN_CSTRING(item->Description, "My desc");
		pm->Proc(item);
		}
		else if (iMode == MODE_READ) {
		if (SLabel == "My label" )
			editObject->objectPtr-> m_Text = Property->chstr;
		}
		else if (iMode == MODE_WRITE) {
		if (SLabel == "My label" )
			ASSIGN_CSTRING(Property->CSPtr, editObject->objectPtr-> m_Text );
	}

	EndProperties(iMode, item, pm);
}
*/

BEGIN_PROPERTY_TABLE();
	PROPERTY_EDIT   (rgbStr,   "Colour filter"  , "Tint the object a different colour (r,g,b format)");
	PROPERTY_VALUE  (opacity, "Opacity", "Description");
	PROPERTY_VALUE  (angle , "Angle" , "Description");
	PROPERTY_BUTTON ("Edit", "Image", "Click to edit the object's image", BtnEditImage());
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

void EditExt::BtnEditImage()
{
	pEditTime->EditImage(imgTexture);
	iTexture = pEditTime->AddImageAsTexture(imgTexture);
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
	pEditTime->Blitrc(pInfo->objectX, pInfo->objectY, pInfo->objectWidth, pInfo->objectHeight, (float)angle, filter);
}

// Initialization:  when the frame editor is opened.  Create textures, fonts,
// and anything else DirectX volatile here.
void EditExt::Initialize()
{
	// Make a texture from the object's image
	iTexture = pEditTime->AddImageAsTexture(imgTexture);
}

void ExtractSubstring(CString& rStr, CString src, char delim, int index)
{
	int c = 0, i;

	// Find beginning of first element
	for (i = 0; i < src.GetLength() && c < index; i++) {
		if (src[i] == delim)
			c++;
	}

	if (i == src.GetLength()) return;

	// Copy out this str (reuse c for character index in rStr)
	c = 0;
	rStr = "";
	
	while (i < src.GetLength()) {

		if (src[i] == delim)
			return;

		rStr += src[i];
		c++;
		i++;
	}
}

void EditExt::OnPropertiesUpdate()
{
	// Parse string in r,g,b format and save in colorref
	CString r;
	CString g;
	CString b;

	ExtractSubstring(r, rgbStr, ',', 0);
	ExtractSubstring(g, rgbStr, ',', 1);
	ExtractSubstring(b, rgbStr, ',', 2);
	
	int red = atoi(r);
	int green = atoi(g);
	int blue = atoi(b);

	filter = D3D_ARGB(opacity, red, green, blue);
}

// When the object is inserted to the frame editor by the user.
void EditExt::OnPut()
{
	imgTexture = pEditTime->AddImage(100,100);
	iTexture = -1;

	pEditTime->EditImage(imgTexture);
	iTexture = pEditTime->AddImageAsTexture(imgTexture);

	rgbStr = "255,255,255";
	opacity = 255;
	filter = D3D_ARGB(255,255,255,255);
	angle = 0;

	pInfo->objectWidth = 100;
	pInfo->objectHeight = 100;
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
	if (ar.loading) {
		//int unused;

		// Code to load your object here
		ar >> angle;
		ar >> filter;
		ar >> imgTexture;
		
		opacity = D3D_A(filter);
	}
	else {
		// Code to save your object here
		ar << angle;
		ar << filter;
		ar << imgTexture;
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