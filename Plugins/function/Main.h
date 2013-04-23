// Main.h
// Definitions and classes for the object workings

// Only include once
#ifndef MAIN_H
#define MAIN_H

#define MINIMUM_VERSION	1
#define IDE_FLAGS 	OF_NODRAW | OF_UNDEFINEDEXPRESSIONS
#include "..\..\Common\ExpReturn.hpp"
#include "..\..\Common\bin.h"

#define OBJECTRECT CRect(editObject->objectX, editObject->objectY, editObject->objectX + editObject->objectWidth, editObject->objectY + editObject->objectHeight)

struct DelayedFunctionCall {
	clock_t scheduledTime;
	CString funcName;
	vector<ExpStore> paramList;
};

// For calling functions in functions
struct FunctionStack {
	CString name;
	vector<ExpStore> paramList;
};

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

	void DebuggerUpdateDisplay(ExpStore*& pPrivateVars) {}
	void OnDebuggerValueChanged(const char* name, const char* value) {}

	////////////////////////////////////////////////////
	// ACTIONS, CONDITIONS AND EXPRESSIONS DEFINITIONS

	long cOnFunction(LPVAL params);
	long cOnAnyFunction(LPVAL params);

	long cAlias(LPVAL params);
	long cOnAlias(LPVAL params);

	long aAddParam(LPVAL params);
	long aClearParams(LPVAL params);
	long aCall(LPVAL params);
	long aCallDelayed(LPVAL params);
	long aSetReturn(LPVAL params);

	long aAliasTrue(LPVAL params);

	long eGetNumParams(LPVAL params, ExpReturn& ret);
	long eGetParam(LPVAL params, ExpReturn& ret);
	long eGetFuncName(LPVAL params, ExpReturn& ret);
	long eGetReturn(LPVAL params, ExpReturn& ret);

	vector<ExpStore> curParams;
	vector<FunctionStack> funcStack;
	list<DelayedFunctionCall> delayedCalls;

	vector<CString> aliasCalls;

	ExpStore retVal;
	bool returned;

	bool aliasflag;		// True when a condition alias returns true
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
	int  GetMenuIcons(int ACEMenu);
	void Initialize();
	void OnPut();
	void OnRemoved();
	int  OnShowACEMenu(TABLE ACEType);
	BOOL OnSizeObject();
	void Serialize(bin& ar);
	void OnPropertiesUpdate();
	void GetAnimationHandle(int) {}

	class VEditTime* pEditTime;	// Pointer to Virtual Edittime
	class editInfo*  pInfo;		// Pointer to object edittime info

	///////////////////////////
	// Your edittime extension data goes here
	int iTexture;		// DX texture
};

// Internal stuff include
#include "..\..\Common\Internal.hpp"
#include "..\..\Common\Properties.h"

// Only include once
#endif