#pragma once

using namespace std;

// contains a very 'basic' loaded version of the obj file, without any optimizations

// --------------------------------------
class face_point_raw
{
public:
	//indexes
	face_point_raw()
	{
		v = -1;
		vt = -1;
		vn = -1;
	}
	int v;
	int vt;
	int vn;
};

class face_raw
{
public:
	vector<face_point_raw> p;
};

class group_raw
{
public:
	group_raw(int _o, int _g);
	int o;
	int g;

	vector<face_raw> f;
};

class obj_raw
{
public:
	obj_raw();
	void load_from_string(stringstream& ss);

public:
	
	vector<cr::point3d> v;
	vector<cr::point> vt;
	vector<cr::point3d> vn;
	vector<string> groups;
	vector<string> objects;

	vector<group_raw> objs;
};

// --------------------------------------

class obj_face_point
{
public:
	cr::point3d* v;
	cr::point* vt;
	cr::point3d* vn;

	obj_face_point()
	{
		v = NULL; vt = NULL; vn = NULL;
	}

	bool operator == (const obj_face_point& o) const
	{
		return (o.v == v && o.vt == vt && o.vn == o.vn);
	}
};


class obj_face
{
public:
	vector<int> indexes;
};




class obj_object
{
public:
	int group;
	int object;
	vector<obj_face> faces;

	// any other per 'group' setting goes here, like texture
};

class obj
{
public:
	obj()
	{
		number_of_indexes = 0;
		indexBuffer = NULL;
		vertexBuffer = NULL;
	}

	void clear();

	vector<cr::point3d> v; // unique vertices
	vector<cr::point> vt; // unique normals
	vector<cr::point3d> vn; // unique normals

	vector<cr::point3d> v_original; // unmodified
	vector<cr::point> vt_original; // unmodified
	vector<cr::point3d> vn_original; // unmodified

	vector<obj_face_point> points;
	list<obj_object> objs;

	int number_of_indexes;

	cr::point3d bb[8]; //8 points to make the bounding box

	vector<string> groups;
	vector<string> objects;
	
	void load_from_string(stringstream& ss);
	void load_from_file(string path);
	void assimp_load_from_mesh(aiMesh& mesh);

	void calculate_bounding_box();

	void scale(float scale);

	void* indexBuffer;
	void* vertexBuffer;
};

class obj_array
{
public:
	void load_from_file(string file);
	vector<obj> objs;

	void calculate_bounding_box();

	cr::point3d bb[8]; //8 points to make the bounding box
};

/*
Okay brain storm.

Firstly, we need to issolate all the vertices that have the same co-ordinates into one map. The purpose
of this is when we apply transformations using the cpu it will have the least number of points to translate

For example, a cube might import with 24 vertices and 24 texture co-ordinates. However only 8 are unique

So first we need to optimise it into 8.

Next, direct x forces us to have texture co-ordinates within the vertices. So we need to loop all the points in the face,
and make a vector than contains a unique association. In the end our 'face' structure will have pointers to 
*/