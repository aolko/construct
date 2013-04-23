// Main.h
// Definitions and classes for the object workings

// Only include once
#ifndef MAIN_H
#define MAIN_H

// Movement object
#define IDE_FLAGS OF_MOVEMENTPLUGIN | OF_NODEBUG


#include "..\..\Common\ExpReturn.hpp"
//#include "..\..\Common\bin.h"

#define OBJECTRECT CRect(editObject->objectX, editObject->objectY, editObject->objectX + editObject->objectWidth, editObject->objectY + editObject->objectHeight)

//////////// RUNTIME OBJECT ////////////
// Add any member functions or data you want to this class.
// Your extension inherits CRunObject.  See the definition
// for the default member values you have access to.
class ExtObject : public CRunObject
{
public:
	// Constructor (called when Construct creates the object)
	ExtObject(initialObject* editObject, VRuntime* pVRuntime);
	// Destructor (called when Construct destroys the object)
	~ExtObject();

	//////////////////////////
	// OnFrame: called once per frame just before Draw() (after the events list)
	// OnFrame2: called once per frame just after Draw() (before the events list)
	BOOL		OnFrame();
	BOOL		OnFrame2();
	// Draw: called when Construct wants you to render your object.
	void		Draw();
	// WindowProc is called if your extension creates a window and calls RegisterWindow().
	LRESULT		WindowProc(HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam);
	BOOL		PreTranslateMessage(MSG* msg);
	long		ReturnDefaultValue(LPVAL theParams, ExpReturn& ret);
	long		ReturnUndefinedExpression(CString& expName, LPVAL theParams, ExpReturn& ret);
	void		OnCreate();
	// Misc functions
	void		OnFrameChange(int oldFrame, int newFrame);
	void		Serialize(bin& ar);
	// Data functions
	long		GetData(int id, void* param);
	long		CallFunction(int id, void* param);
	// Debugger functions
	void	DebuggerUpdateDisplay(ExpStore*& pPrivateVars);
	void	OnDebuggerValueChanged(const char* name, const char* value);

	////////////////////////////////////////////////////
	// ACTIONS, CONDITIONS AND EXPRESSIONS DEFINITIONS
	long cTrigger(LPVAL params);
	long cIsDragging(LPVAL params);
	long aSetActivated(LPVAL theParams);
	long aForceDragDrop(LPVAL theParams);
const bool IsMouseOverWindow(HWND hWnd, const int mx, const int my,
                          const bool inClientSpace /*= false */);

bool allowoff;


	bool mouse[3];	// L, M, R
	int mouseButton;

	bool activated;

	bool dragging;
	int dx, dy;
	bool ignore;

	int axis;

	// limitations of axis
	int x_limit;
	int y_limit;
	int x_gone;
	int y_gone;

	void CheckStartDrag(int button);
	void CheckEndDrag(int button);

	// For finding topmost object
	bool want_to_start;

	static vector<CRunObject*> readyInstances;

};

//////////// EDITTIME INFO ////////////
class EditExt
{
public:
	///////////////////////////
	// Class data
	EditExt(class VEditTime* pVEditTime, class editInfo* pEInfo);
	~EditExt();

	void Draw();
	void Initialize();
	void OnPut();
	void OnRemoved();
	BOOL OnSizeObject();
	void Serialize(bin& ar);
	void OnPropertiesUpdate();
	void GetAnimationHandle(int&) {}

	class VEditTime* pEditTime;	// Pointer to Virtual Edittime
	class editInfo*  pInfo;		// Pointer to object edittime info

	int mouse_button;
	int axis;
	int allowoff;

	int x_limit;
	int y_limit;
};

// Internal stuff include
#include "..\..\Common\Internal.hpp"
#include "..\..\Common\Properties.h"

// Only include once
#endif