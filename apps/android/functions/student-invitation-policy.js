export const normalizeStudentCode=value=>typeof value==='string'?value.toUpperCase().replace(/[\s-]/g,''):'';
export function studentMembership(profile,classId){
 if(profile.isInstructor===true||profile.isAdmin===true)throw new Error('Use instructor tools to manage instructor classrooms.');
 if((profile.removedClassIds||[]).includes(classId))throw new Error('An instructor must restore your access to this classroom.');
 return {classIds:[...new Set([...(profile.classIds||[]),classId])],activeClassId:classId,classId};
}
