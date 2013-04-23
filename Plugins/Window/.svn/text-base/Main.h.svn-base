// Main.h
// Definitions and classes for the object workings

// Only include once
#ifndef MAIN_H
#define MAIN_H

#define IDE_FLAGS OF_NODRAW | OF_NOCOMMONDEBUG

#include "..\..\Common\ExpReturn.hpp"

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
	// Debugging
	void		DebuggerUpdateDisplay(ExpStore*& pPrivateVars);
	void		OnDebuggerValueChanged(const char* name, const char* value);

	// Actions
	long aAttach				(LPVAL theParams);
	long aCloseWindow			(LPVAL theParams);
	long aHideWindow			(LPVAL theParams);
	long aShowWindow			(LPVAL theParams);
	long aFlashWindow			(LPVAL theParams);
	long aSetWindowTitle		(LPVAL theParams);
	long aMinimizeWindow		(LPVAL theParams);
	long aMaximizeWindow		(LPVAL theParams);
	long aAlwaysOnTop			(LPVAL theParams);
	long aBringToTop			(LPVAL theParams);
	long aSetX					(LPVAL theParams);
	long aSetY					(LPVAL theParams);
	long aSetPosition			(LPVAL theParams);
	long aCenterWindow			(LPVAL theParams);
	long aSetHeight				(LPVAL theParams);
	long aSetWidth				(LPVAL theParams);
	long aSetSize				(LPVAL theParams);

	long aSetClientHeight		(LPVAL theParams);
	long aSetClientWidth		(LPVAL theParams);
	long aSetClientSize			(LPVAL theParams);

	long aAttachToCurrent		(LPVAL theParams);
	long aRestoreWindow			(LPVAL theParams);
	// long aUnmaximizeWindow(LPVAL theParams); // not implemented. It isn't needed anyway, because aRestoreWindow
												// also "unmaximizes" a maximized window.

	// Expressions
	long eFindWindow			(LPVAL theParams, ExpReturn& ret);
	long eGetApplicationWindow	(LPVAL theParams, ExpReturn& ret);
	long eGetWindows			(LPVAL theParams, ExpReturn& ret);
	long eGetWindowTitle		(LPVAL theParams, ExpReturn& ret);
	long eGetX					(LPVAL theParams, ExpReturn& ret);
	long eGetY					(LPVAL theParams, ExpReturn& ret);
	long eGetWidth				(LPVAL theParams, ExpReturn& ret);
	long eGetHeight				(LPVAL theParams, ExpReturn& ret);
	long eGetClientX			(LPVAL theParams, ExpReturn& ret);
	long eGetClientY			(LPVAL theParams, ExpReturn& ret);
	long eGetClientWidth		(LPVAL theParams, ExpReturn& ret);
	long eGetClientHeight		(LPVAL theParams, ExpReturn& ret);
	
	vector<ExpStore> Array;
	ExpStore xyRet[2];	// Position & size

	HWND m_Window;
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
};

// Internal stuff include
#include "..\..\Common\Internal.hpp"
#include "..\..\Common\Properties.h"

// Only include once
#endif