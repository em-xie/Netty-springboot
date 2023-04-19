
function getIM(){
    //  UNIAPP 环境
    if(typeof uni !== 'undefined'){
        return {im:uni['im'], im_webtoolkit:uni['im_webtoolkit']};
    }
    throw Error(" Couldn't not fetch lim.");
}


const lim = {
    im: getIM().im,
};

module.exports = lim;
